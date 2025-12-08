package ingsist.snippet.test.service

import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.dtos.PermissionDTO
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import ingsist.snippet.test.TestUsers
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@DisplayName("SnippetTestService - List")
class SnippetTestServiceListTest {
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

    @Test
    fun `allows collaborator with permission`() {
        val snippet = snippet(ownerId = snippetOwner.id)
        val collaborator = TestUsers.COLLABORATOR
        val storedTest = snippetTest(snippet, versionTag = "1.0")

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(authService.getSharedSnippets(collaborator.id, collaborator.token))
            .thenReturn(shared(snippetId, collaborator.id))
        whenever(snippetTestRepository.findAllBySnippetId(snippetId)).thenReturn(listOf(storedTest))

        val tests = service.listTests(snippetId, collaborator.id, collaborator.token)

        assertEquals(1, tests.size)
        assertEquals(storedTest.name, tests.first().name)
    }

    @Test
    fun `owner listing skips auth client`() {
        val snippet = snippet(ownerId = snippetOwner.id)
        val storedTest = snippetTest(snippet, versionTag = "1.0")

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(snippetTestRepository.findAllBySnippetId(snippetId)).thenReturn(listOf(storedTest))

        val tests = service.listTests(snippetId, snippetOwner.id, snippetOwner.token)

        assertEquals(1, tests.size)
        verifyNoInteractions(authService)
    }

    @Test
    fun `rejects collaborator without permission`() {
        val snippet = snippet(ownerId = snippetOwner.id)
        val collaborator = TestUsers.COLLABORATOR

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(authService.getSharedSnippets(collaborator.id, collaborator.token)).thenReturn(emptyList())

        assertThrows(SnippetAccessDeniedException::class.java) {
            service.listTests(snippetId, collaborator.id, collaborator.token)
        }
    }

    @Test
    fun `propagates not found when snippet is missing`() {
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        assertThrows(SnippetNotFoundException::class.java) {
            service.listTests(snippetId, snippetOwner.id, snippetOwner.token)
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
