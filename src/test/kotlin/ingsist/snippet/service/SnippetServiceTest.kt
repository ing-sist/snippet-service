package ingsist.snippet.service

import ingsist.snippet.asset.AssetService
import ingsist.snippet.client.AuthClient
import ingsist.snippet.domain.SnippetMetadata
import ingsist.snippet.engine.EngineService
import ingsist.snippet.exception.SnippetAccessDeniedException
import ingsist.snippet.exception.SnippetNotFoundException
import ingsist.snippet.repository.SnippetRepository
import ingsist.snippet.repository.SnippetVersionRepository
import ingsist.snippet.test.TestUsers
import ingsist.snippet.test.snippetMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@DisplayName("SnippetService")
class SnippetServiceTest {
    @Mock
    private lateinit var snippetRepository: SnippetRepository

    @Mock
    private lateinit var snippetVersionRepository: SnippetVersionRepository

    @Mock
    private lateinit var assetService: AssetService

    @Mock
    private lateinit var engineService: EngineService

    @Mock
    private lateinit var authClient: AuthClient

    private lateinit var snippetService: SnippetService

    @BeforeEach
    fun setUp() {
        snippetService =
            SnippetService(
                snippetRepository,
                snippetVersionRepository,
                assetService,
                engineService,
                authClient,
            )
    }

    @Nested
    @DisplayName("US #5: List All Snippets (Own + Shared)")
    inner class ListSnippetsTests {
        @Test
        @DisplayName("GIVEN user has own snippets WHEN listing snippets THEN returns only own snippets")
        fun `returns own snippets when no shared`() {
            // Given
            val user = TestUsers.OWNER
            val ownSnippet1 =
                snippetMetadata {
                    name = "My First Snippet"
                    ownerId = user.id
                }
            val ownSnippet2 =
                snippetMetadata {
                    name = "My Second Snippet"
                    ownerId = user.id
                }
            val pageable = PageRequest.of(0, 10)
            val page = PageImpl(listOf(ownSnippet1, ownSnippet2), pageable, 2)

            whenever(authClient.getUserPermissions(user.id, user.token)).thenReturn(emptyList())
            whenever(snippetRepository.findAllByOwnerId(user.id, pageable)).thenReturn(page)

            // When
            val result = snippetService.getAllSnippets(user.id, 0, 10, user.token)

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.ownerId == user.id })
            verify(snippetRepository).findAllByOwnerId(user.id, pageable)
        }

        @Test
        @DisplayName("GIVEN user has shared snippets WHEN listing snippets THEN returns own and shared snippets")
        fun `returns own and shared snippets`() {
            // Given
            val user = TestUsers.OWNER
            val sharedSnippetId = UUID.randomUUID()
            val ownSnippet =
                snippetMetadata {
                    name = "Own Snippet"
                    ownerId = user.id
                }
            val sharedSnippet =
                snippetMetadata {
                    id = sharedSnippetId
                    name = "Shared Snippet"
                    ownerId = TestUsers.STRANGER.id
                }
            val pageable = PageRequest.of(0, 10)
            val page = PageImpl(listOf(ownSnippet, sharedSnippet), pageable, 2)

            whenever(authClient.getUserPermissions(user.id, user.token)).thenReturn(listOf(sharedSnippetId))
            whenever(snippetRepository.findAllByOwnerIdOrIdIn(user.id, listOf(sharedSnippetId), pageable))
                .thenReturn(page)

            // When
            val result = snippetService.getAllSnippets(user.id, 0, 10, user.token)

            // Then
            assertEquals(2, result.size)
            assertTrue(result.any { it.name == "Own Snippet" })
            assertTrue(result.any { it.name == "Shared Snippet" })
        }

        @Test
        @DisplayName("GIVEN user has no snippets WHEN listing snippets THEN returns empty list")
        fun `returns empty list when no snippets`() {
            // Given
            val user = TestUsers.OWNER
            val pageable = PageRequest.of(0, 10)
            val emptyPage = PageImpl<SnippetMetadata>(emptyList(), pageable, 0)

            whenever(authClient.getUserPermissions(user.id, user.token)).thenReturn(emptyList())
            whenever(snippetRepository.findAllByOwnerId(user.id, pageable)).thenReturn(emptyPage)

            // When
            val result = snippetService.getAllSnippets(user.id, 0, 10, user.token)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("GIVEN pagination parameters WHEN listing snippets THEN respects pagination")
        fun `respects pagination parameters`() {
            // Given
            val user = TestUsers.OWNER
            val pageable = PageRequest.of(2, 5)
            val snippets =
                (1..5).map {
                    snippetMetadata {
                        name = "Snippet $it"
                        ownerId = user.id
                    }
                }
            val page = PageImpl(snippets, pageable, 15)

            whenever(authClient.getUserPermissions(user.id, user.token)).thenReturn(emptyList())
            whenever(snippetRepository.findAllByOwnerId(user.id, pageable)).thenReturn(page)

            // When
            val result = snippetService.getAllSnippets(user.id, 2, 5, user.token)

            // Then
            assertEquals(5, result.size)
            verify(snippetRepository).findAllByOwnerId(user.id, pageable)
        }
    }

    @Nested
    @DisplayName("US #6: Get Snippet Details")
    inner class GetSnippetDetailsTests {
        @Test
        @DisplayName("GIVEN snippet exists WHEN getting by ID THEN returns snippet details")
        fun `returns snippet when exists`() {
            // Given
            val snippetId = UUID.randomUUID()
            val snippet =
                snippetMetadata {
                    id = snippetId
                    name = "My Snippet"
                    language = "printscript"
                    description = "Test description"
                    ownerId = TestUsers.OWNER.id
                }

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))

            // When
            val result = snippetService.getSnippetById(snippetId)

            // Then
            assertNotNull(result)
            assertEquals(snippetId, result.id)
            assertEquals("My Snippet", result.name)
            assertEquals("printscript", result.language)
            assertEquals("Test description", result.description)
        }

        @Test
        @DisplayName("GIVEN snippet does not exist WHEN getting by ID THEN throws SnippetNotFoundException")
        fun `throws exception when snippet not found`() {
            // Given
            val nonExistentId = UUID.randomUUID()
            whenever(snippetRepository.findById(nonExistentId)).thenReturn(Optional.empty())

            // When & Then
            val exception =
                assertThrows(SnippetNotFoundException::class.java) {
                    snippetService.getSnippetById(nonExistentId)
                }
            assertTrue(exception.message!!.contains(nonExistentId.toString()))
        }

        @Test
        @DisplayName("GIVEN snippet with version WHEN getting by ID THEN returns correct version tag")
        fun `returns correct version tag`() {
            // Given
            val snippetId = UUID.randomUUID()
            val snippet =
                snippetMetadata {
                    id = snippetId
                    name = "Versioned Snippet"
                    ownerId = TestUsers.OWNER.id
                }

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))

            // When
            val result = snippetService.getSnippetById(snippetId)

            // Then
            assertEquals("1.0", result.version)
            assertEquals("pending", result.compliance)
        }
    }

    @Nested
    @DisplayName("US #7: Share Snippet")
    inner class ShareSnippetTests {
        @Test
        @DisplayName("GIVEN user is owner WHEN sharing snippet THEN shares successfully")
        fun `shares snippet when user is owner`() {
            // Given
            val owner = TestUsers.OWNER
            val collaborator = TestUsers.COLLABORATOR
            val snippetId = UUID.randomUUID()
            val snippet =
                snippetMetadata {
                    id = snippetId
                    ownerId = owner.id
                }

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
            doNothing().whenever(authClient).shareSnippet(snippetId, collaborator.id, owner.token)

            // When
            snippetService.shareSnippet(snippetId, collaborator.id, owner.id, owner.token)

            // Then
            verify(authClient).shareSnippet(snippetId, collaborator.id, owner.token)
        }

        @Test
        @DisplayName("GIVEN snippet does not exist WHEN sharing THEN throws SnippetNotFoundException")
        fun `throws exception when snippet not found`() {
            // Given
            val owner = TestUsers.OWNER
            val nonExistentId = UUID.randomUUID()

            whenever(snippetRepository.findById(nonExistentId)).thenReturn(Optional.empty())

            // When & Then
            val exception =
                assertThrows(SnippetNotFoundException::class.java) {
                    snippetService.shareSnippet(nonExistentId, TestUsers.COLLABORATOR.id, owner.id, owner.token)
                }
            assertTrue(exception.message!!.contains(nonExistentId.toString()))
            verify(authClient, never()).shareSnippet(any(), any(), any())
        }

        @Test
        @DisplayName("GIVEN user is not owner WHEN sharing snippet THEN throws SnippetAccessDeniedException")
        fun `throws exception when user is not owner`() {
            // Given
            val owner = TestUsers.OWNER
            val stranger = TestUsers.STRANGER
            val snippetId = UUID.randomUUID()
            val snippet =
                snippetMetadata {
                    id = snippetId
                    ownerId = owner.id
                }

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))

            // When & Then
            assertThrows(SnippetAccessDeniedException::class.java) {
                snippetService.shareSnippet(snippetId, TestUsers.COLLABORATOR.id, stranger.id, stranger.token)
            }
            verify(authClient, never()).shareSnippet(any(), any(), any())
        }

        @Test
        @DisplayName("GIVEN valid share request WHEN sharing THEN calls auth client with correct parameters")
        fun `calls auth client with correct parameters`() {
            // Given
            val owner = TestUsers.OWNER
            val collaborator = TestUsers.COLLABORATOR
            val snippetId = UUID.randomUUID()
            val snippet =
                snippetMetadata {
                    id = snippetId
                    ownerId = owner.id
                }

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
            doNothing().whenever(authClient).shareSnippet(snippetId, collaborator.id, owner.token)

            // When
            snippetService.shareSnippet(snippetId, collaborator.id, owner.id, owner.token)

            // Then
            verify(authClient).shareSnippet(eq(snippetId), eq(collaborator.id), eq(owner.token))
        }
    }
}
