package ingsist.snippet.test.service

import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import ingsist.snippet.test.model.dto.CreateTestRequest
import ingsist.snippet.test.model.dto.FailureDetail
import ingsist.snippet.test.model.dto.RunStatus
import ingsist.snippet.test.model.dto.RunTestResponse
import ingsist.snippet.test.model.dto.SnippetTestResponse
import ingsist.snippet.test.model.entity.SnippetTest
import ingsist.snippet.test.repository.SnippetTestRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SnippetTestService(
    private val snippetRepository: SnippetRepository,
    private val snippetVersionRepository: SnippetVersionRepository,
    private val snippetTestRepository: SnippetTestRepository,
    private val authService: AuthService,
    private val engineService: EngineService,
) {
    fun createTest(
        snippetId: UUID,
        ownerId: String,
        request: CreateTestRequest,
    ): SnippetTestResponse {
        val snippet = snippet(snippetId)
        requireOwner(snippet, ownerId)
        val versionTag = latestVersionTag(snippet.id)
        val saved =
            snippetTestRepository.save(
                SnippetTest(
                    testId = UUID.randomUUID(),
                    name = request.name,
                    snippet = snippet,
                    versionTag = versionTag,
                    inputs = request.inputs,
                    expectedOutputs = request.expectedOutputs,
                ),
            )
        return saved.toResponse()
    }

    fun listTests(
        snippetId: UUID,
        requesterId: String,
        token: String,
    ): List<SnippetTestResponse> {
        val snippet = snippet(snippetId)
        requireAccess(snippet, requesterId, token)
        return snippetTestRepository.findAllBySnippetId(snippetId).map { it.toResponse() }
    }

    fun deleteTest(
        snippetId: UUID,
        testId: UUID,
        ownerId: String,
    ) {
        val snippet = snippet(snippetId)
        requireOwner(snippet, ownerId)
        snippetTestRepository.delete(test(snippetId, testId))
    }

    fun runTest(
        snippetId: UUID,
        testId: UUID,
        requesterId: String,
        token: String,
    ): RunTestResponse {
        val snippet = snippet(snippetId)
        requireAccess(snippet, requesterId, token)
        val test = test(snippetId, testId)
        val version =
            snippetVersionRepository.findBySnippetIdAndVersionTag(snippetId, test.versionTag)
                ?: return missingVersion(test)

        val execution = execute(snippet, version.versionTag, test.inputs)
        val outputFailures = compareOutputs(test.expectedOutputs, execution.outputs)
        val status = if (execution.errors.isEmpty() && outputFailures.isEmpty()) RunStatus.SUCCESS else RunStatus.FAIL
        val engineFailures =
            if (execution.errors.isEmpty()) {
                emptyList()
            } else {
                listOf(FailureDetail(index = -1, expected = null, obtained = null, reason = "Engine returned errors"))
            }

        return RunTestResponse(
            status = status,
            outputs = execution.outputs,
            errors = execution.errors,
            failures = outputFailures + engineFailures,
            version = version.versionTag,
        )
    }

    private fun snippet(snippetId: UUID): SnippetMetadata =
        snippetRepository.findById(snippetId).orElseThrow {
            SnippetNotFoundException("Snippet with id $snippetId not found")
        }

    private fun test(
        snippetId: UUID,
        testId: UUID,
    ): SnippetTest =
        snippetTestRepository.findBySnippetIdAndTestId(snippetId, testId)
            ?: throw SnippetNotFoundException("Test with id $testId not found for snippet $snippetId")

    private fun latestVersionTag(snippetId: UUID): String =
        snippetVersionRepository.findFirstBySnippetIdOrderByCreatedDateDesc(snippetId)?.versionTag
            ?: throw SnippetNotFoundException("Snippet with id $snippetId has no versions")

    private fun requireOwner(
        snippet: SnippetMetadata,
        userId: String,
    ) {
        if (snippet.ownerId != userId) throw SnippetAccessDeniedException("You are not the owner of this snippet")
    }

    private fun requireAccess(
        snippet: SnippetMetadata,
        userId: String,
        token: String,
    ) {
        if (snippet.ownerId == userId) return
        val sharedSnippetIds = authService.getSharedSnippets(userId, token).map { it.snippetId }
        if (!sharedSnippetIds.contains(snippet.id)) {
            throw SnippetAccessDeniedException("You don't have access to this snippet")
        }
    }

    private fun execute(
        snippet: SnippetMetadata,
        versionTag: String,
        inputs: List<String>,
    ): EngineExecution {
        val response =
            engineService.execute(
                ExecuteReqDTO(
                    snippetId = snippet.id,
                    inputs = inputs.toMutableList(),
                    version = versionTag,
                    language = snippet.language,
                ),
            )
        return EngineExecution(outputs = response.outputs, errors = response.errors)
    }

    private fun missingVersion(test: SnippetTest) =
        RunTestResponse(
            status = RunStatus.FAIL,
            outputs = emptyList(),
            errors = emptyList(),
            failures =
                listOf(
                    FailureDetail(
                        index = -1,
                        expected = null,
                        obtained = null,
                        reason = "Version ${test.versionTag} not found for snippet ${test.snippet.id}",
                    ),
                ),
            version = test.versionTag,
        )
}

private fun SnippetTest.toResponse(): SnippetTestResponse =
    SnippetTestResponse(
        id = testId,
        name = name,
        inputs = inputs,
        expectedOutputs = expectedOutputs,
        version = versionTag,
    )

private fun compareOutputs(
    expected: List<String>,
    actual: List<String>,
): List<FailureDetail> {
    val failures = mutableListOf<FailureDetail>()
    val maxIndex = maxOf(expected.size, actual.size)
    for (index in 0 until maxIndex) {
        val expectedValue = expected.getOrNull(index)
        val obtainedValue = actual.getOrNull(index)
        when {
            expectedValue == null ->
                failures.add(
                    FailureDetail(
                        index = index,
                        expected = null,
                        obtained = obtainedValue,
                        reason = "Unexpected output at position $index",
                    ),
                )

            obtainedValue == null ->
                failures.add(
                    FailureDetail(
                        index = index,
                        expected = expectedValue,
                        obtained = null,
                        reason = "Missing output at position $index",
                    ),
                )

            expectedValue != obtainedValue ->
                failures.add(
                    FailureDetail(
                        index = index,
                        expected = expectedValue,
                        obtained = obtainedValue,
                        reason = "Mismatch at position $index",
                    ),
                )
        }
    }
    return failures
}

private data class EngineExecution(
    val outputs: List<String>,
    val errors: List<String>,
)
