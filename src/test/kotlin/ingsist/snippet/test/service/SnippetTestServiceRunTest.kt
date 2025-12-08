package ingsist.snippet.test.service

import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.domain.SnippetVersion
import ingsist.snippet.runner.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.runner.snippet.dtos.ExecuteResDTO
import ingsist.snippet.runner.snippet.dtos.PermissionDTO
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import ingsist.snippet.test.TestUsers
import ingsist.snippet.test.model.dto.RunStatus
import ingsist.snippet.test.model.entity.SnippetTest
import ingsist.snippet.test.repository.SnippetTestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@DisplayName("SnippetTestService - Run")
class SnippetTestServiceRunTest {
    @Mock private lateinit var snippetRepository: SnippetRepository

    @Mock private lateinit var snippetVersionRepository: SnippetVersionRepository

    @Mock private lateinit var snippetTestRepository: SnippetTestRepository

    @Mock private lateinit var authService: AuthService

    @Mock private lateinit var engineService: EngineService

    private lateinit var service: ingsist.snippet.test.service.SnippetTestService
    private val snippetOwner = TestUsers.OWNER
    private val snippetId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            ingsist.snippet.test.service.SnippetTestService(
                snippetRepository,
                snippetVersionRepository,
                snippetTestRepository,
                authService,
                engineService,
            )
    }

    @Test
    fun `returns success when engine outputs match expected`() {
        val snippet = snippet(ownerId = snippetOwner.id)
        val version = version(snippet, tag = "1.0", assetKey = "asset-key")
        val savedTest =
            snippetTest(
                snippet,
                versionTag = version.versionTag,
                expectedOutputs = listOf("Hola"),
            )
        val execution =
            ExecuteResDTO(
                snippetId = snippetId,
                outputs = listOf("Hola"),
                errors = emptyList(),
            )

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(
            snippetTestRepository.findBySnippetIdAndTestId(snippetId, savedTest.testId),
        ).thenReturn(savedTest)
        whenever(
            snippetVersionRepository.findBySnippetIdAndVersionTag(snippetId, version.versionTag),
        ).thenReturn(version)
        whenever(engineService.getSnippetContent(version.assetKey)).thenReturn("printscript code")
        whenever(engineService.execute(any<ExecuteReqDTO>())).thenReturn(execution)

        val result =
            service.runTest(
                snippetId,
                savedTest.testId,
                snippetOwner.id,
                snippetOwner.token,
            )

        assertEquals(RunStatus.SUCCESS, result.status)
        assertEquals(0, result.failures.size)
        assertEquals(listOf("Hola"), result.outputs)
    }

    @Test
    fun `returns failure when referenced version is missing`() {
        val snippet = snippet(ownerId = snippetOwner.id)
        val savedTest = snippetTest(snippet, versionTag = "9.9")

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(
            snippetTestRepository.findBySnippetIdAndTestId(snippetId, savedTest.testId),
        ).thenReturn(savedTest)
        whenever(
            snippetVersionRepository.findBySnippetIdAndVersionTag(snippetId, "9.9"),
        ).thenReturn(null)

        val result =
            service.runTest(
                snippetId,
                savedTest.testId,
                snippetOwner.id,
                snippetOwner.token,
            )

        assertEquals(RunStatus.FAIL, result.status)
        assertEquals("9.9", result.version)
        assertEquals(emptyList<String>(), result.errors)
        assertEquals(
            "Version 9.9 not found for snippet ${snippet.id}",
            result.failures.first().reason,
        )
    }

    @Test
    fun `reports mismatched outputs`() {
        val snippet = snippet(ownerId = snippetOwner.id)
        val version = version(snippet, tag = "1.0", assetKey = "asset-key")
        val savedTest =
            snippetTest(
                snippet,
                versionTag = version.versionTag,
                expectedOutputs = listOf("Hola", "Chau"),
            )
        val execution =
            ExecuteResDTO(
                snippetId = snippetId,
                outputs = listOf("Hola", "Adios"),
                errors = emptyList(),
            )

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(
            snippetTestRepository.findBySnippetIdAndTestId(snippetId, savedTest.testId),
        ).thenReturn(savedTest)
        whenever(
            snippetVersionRepository.findBySnippetIdAndVersionTag(snippetId, version.versionTag),
        ).thenReturn(version)
        whenever(engineService.getSnippetContent(version.assetKey)).thenReturn("printscript code")
        whenever(engineService.execute(any<ExecuteReqDTO>())).thenReturn(execution)

        val result =
            service.runTest(
                snippetId,
                savedTest.testId,
                snippetOwner.id,
                snippetOwner.token,
            )

        assertEquals(RunStatus.FAIL, result.status)
        assertEquals(1, result.failures.size)
        val failure = result.failures.first()
        assertEquals(1, failure.index)
        assertEquals("Chau", failure.expected)
        assertEquals("Adios", failure.obtained)
        assertEquals("Mismatch at position 1", failure.reason)
    }

    @Test
    fun `reports engine errors as failure`() {
        val snippet = snippet(ownerId = snippetOwner.id)
        val version = version(snippet, tag = "1.0", assetKey = "asset-key")
        val savedTest =
            snippetTest(
                snippet,
                versionTag = version.versionTag,
                expectedOutputs = listOf("Hola"),
            )
        val execution =
            ExecuteResDTO(
                snippetId = snippetId,
                outputs = emptyList(),
                errors = listOf("Runtime error"),
            )

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(
            snippetTestRepository.findBySnippetIdAndTestId(snippetId, savedTest.testId),
        ).thenReturn(savedTest)
        whenever(
            snippetVersionRepository.findBySnippetIdAndVersionTag(snippetId, version.versionTag),
        ).thenReturn(version)
        whenever(engineService.getSnippetContent(version.assetKey)).thenReturn("printscript code")
        whenever(engineService.execute(any<ExecuteReqDTO>())).thenReturn(execution)

        val result =
            service.runTest(
                snippetId,
                savedTest.testId,
                snippetOwner.id,
                snippetOwner.token,
            )

        assertEquals(RunStatus.FAIL, result.status)
        assertEquals(listOf("Runtime error"), result.errors)
        assertEquals(2, result.failures.size)
        val missingOutput = result.failures.first { it.index == 0 }
        assertEquals("Missing output at position 0", missingOutput.reason)
        val engineFailure = result.failures.first { it.index == -1 }
        assertEquals("Engine returned errors", engineFailure.reason)
    }

    @Test
    fun `throws when snippet does not exist`() {
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        assertThrows(SnippetNotFoundException::class.java) {
            service.runTest(snippetId, UUID.randomUUID(), snippetOwner.id, snippetOwner.token)
        }
    }

    @Test
    fun `rejects run when collaborator lacks permissions`() {
        val snippet = snippet(ownerId = snippetOwner.id)

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(
            authService.getSharedSnippets(TestUsers.COLLABORATOR.id, TestUsers.COLLABORATOR.token),
        ).thenReturn(emptyList())

        assertThrows(SnippetAccessDeniedException::class.java) {
            service.runTest(snippetId, UUID.randomUUID(), TestUsers.COLLABORATOR.id, TestUsers.COLLABORATOR.token)
        }
        verify(snippetTestRepository, never()).findBySnippetIdAndTestId(any(), any())
    }

    private fun snippet(ownerId: String) =
        SnippetMetadata(
            id = snippetId,
            name = "printscript sample",
            language = "printscript",
            langVersion = "1.0",
            description = "demo snippet",
            ownerId = ownerId,
        )

    private fun version(
        snippet: SnippetMetadata,
        tag: String,
        assetKey: String = "snippet-${snippet.id}.ps",
    ) = SnippetVersion(
        versionId = UUID.randomUUID(),
        assetKey = assetKey,
        createdDate = Date(),
        versionTag = tag,
        snippet = snippet,
    )

    private fun snippetTest(
        snippet: SnippetMetadata,
        versionTag: String,
        expectedOutputs: List<String> = listOf("output"),
    ) = SnippetTest(
        testId = UUID.randomUUID(),
        name = "simple test",
        snippet = snippet,
        versionTag = versionTag,
        inputs = emptyList(),
        expectedOutputs = expectedOutputs,
    )

    private fun shared(
        snippetId: UUID,
        userId: String,
    ) = listOf(PermissionDTO(userId = userId, snippetId = snippetId, permission = "READ"))
}
