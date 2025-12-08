package ingsist.snippet.test.service

import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.domain.SnippetVersion
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import ingsist.snippet.test.TestUsers
import ingsist.snippet.test.model.dto.CreateTestRequest
import ingsist.snippet.test.model.entity.SnippetTest
import ingsist.snippet.test.repository.SnippetTestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
@DisplayName("SnippetTestService - Create & Delete")
class SnippetTestServiceCreateDeleteTest {
    @Mock private lateinit var snippetRepository: SnippetRepository

    @Mock private lateinit var snippetVersionRepository: SnippetVersionRepository

    @Mock private lateinit var snippetTestRepository: SnippetTestRepository

    @Mock private lateinit var authService: AuthService

    @Mock private lateinit var engineService: ingsist.snippet.engine.EngineService

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

    @Nested
    inner class CreateTestCases {
        @Test
        fun `stores test for latest version when requester is owner`() {
            val snippet = snippet(ownerId = snippetOwner.id)
            val latestVersion = version(snippet, tag = "1.0")
            val request =
                CreateTestRequest(
                    name = "sums numbers",
                    inputs = listOf("1 2"),
                    expectedOutputs = listOf("3"),
                )

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
            whenever(
                snippetVersionRepository.findFirstBySnippetIdOrderByCreatedDateDesc(snippetId),
            ).thenReturn(latestVersion)
            whenever(snippetTestRepository.save(any<SnippetTest>())).thenAnswer { it.arguments[0] }

            val response = service.createTest(snippetId, snippetOwner.id, request)

            assertEquals("sums numbers", response.name)
            assertEquals(listOf("1 2"), response.inputs)
            assertEquals(listOf("3"), response.expectedOutputs)
            assertEquals("1.0", response.version)
        }

        @Test
        fun `rejects creation when requester is not owner`() {
            val snippet = snippet(ownerId = snippetOwner.id)
            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))

            assertThrows(SnippetAccessDeniedException::class.java) {
                service.createTest(snippetId, TestUsers.STRANGER.id, CreateTestRequest("forbidden"))
            }
            verify(snippetTestRepository, never()).save(any())
        }

        @Test
        fun `fails when snippet has no versions`() {
            val snippet = snippet(ownerId = snippetOwner.id)
            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
            whenever(snippetVersionRepository.findFirstBySnippetIdOrderByCreatedDateDesc(snippetId)).thenReturn(null)

            assertThrows(SnippetNotFoundException::class.java) {
                service.createTest(snippetId, snippetOwner.id, CreateTestRequest("no version"))
            }
            verify(snippetTestRepository, never()).save(any())
        }
    }

    @Nested
    inner class DeleteTestCases {
        @Test
        fun `owner can delete test`() {
            val snippet = snippet(ownerId = snippetOwner.id)
            val savedTest = snippetTest(snippet, versionTag = "1.0")

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
            whenever(snippetTestRepository.findBySnippetIdAndTestId(snippetId, savedTest.testId)).thenReturn(savedTest)

            service.deleteTest(snippetId, savedTest.testId, snippetOwner.id)

            verify(snippetTestRepository).delete(savedTest)
        }

        @Test
        fun `rejects delete for non owner`() {
            val snippet = snippet(ownerId = snippetOwner.id)
            val savedTest = snippetTest(snippet, versionTag = "1.0")

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))

            assertThrows(SnippetAccessDeniedException::class.java) {
                service.deleteTest(snippetId, savedTest.testId, TestUsers.STRANGER.id)
            }
            verify(snippetTestRepository, never()).delete(any())
        }
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
}
