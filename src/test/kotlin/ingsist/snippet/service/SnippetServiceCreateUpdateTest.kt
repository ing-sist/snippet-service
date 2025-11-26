package ingsist.snippet.service

import ingsist.snippet.asset.AssetService
import ingsist.snippet.client.AuthClient
import ingsist.snippet.domain.SnippetMetadata
import ingsist.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.domain.SnippetVersion
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.ExecuteResDTO
import ingsist.snippet.engine.EngineService
import ingsist.snippet.repository.SnippetRepository
import ingsist.snippet.repository.SnippetVersionRepository
import ingsist.snippet.test.TestUsers
import ingsist.snippet.test.snippetMetadata
import ingsist.snippet.test.submitSnippet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

/**
 * Tests for SnippetService - Create and Update operations
 * US #1: Upload Snippet from File - Input: file content
 * US #2: Update Snippet Code - Input: new code, snippet ID
 * US #3: Upload Snippet Inline - Input: code text
 * US #4: Validate Snippet - Input: code to validate
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("SnippetService - Create/Update Operations")
class SnippetServiceCreateUpdateTest {
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
    @DisplayName("US #1 & #3: Create Snippet")
    inner class CreateSnippetTests {
        @Test
        @DisplayName("GIVEN valid snippet WHEN creating THEN returns success with snippet ID")
        fun `creates snippet successfully with valid data`() {
            // Given
            val owner = TestUsers.OWNER
            val snippet =
                submitSnippet {
                    code = "let x: number = 1;"
                    name = "My First Snippet"
                    language = "printscript"
                    version = "1.1"
                    description = "A simple variable declaration"
                    versionTag = "1.0"
                }

            val validResponse =
                ExecuteResDTO(
                    snippetId = UUID.randomUUID(),
                    outputs = emptyList(),
                    errors = emptyList(),
                )

            whenever(engineService.parse(any<ExecuteReqDTO>())).thenReturn(validResponse)
            whenever(assetService.upload(anyString(), anyString(), anyString())).thenReturn("Asset uploaded")
            whenever(snippetRepository.save(any<SnippetMetadata>())).thenAnswer { it.arguments[0] }
            whenever(snippetVersionRepository.save(any<SnippetVersion>())).thenAnswer { it.arguments[0] }

            // When
            val result = snippetService.createSnippet(snippet, owner.id)

            // Then
            assertTrue(result is SnippetSubmissionResult.Success)
            val success = result as SnippetSubmissionResult.Success
            assertEquals("My First Snippet", success.name)
            assertEquals("printscript", success.language)
            assertEquals("1.0", success.version)
            assertNotNull(success.snippetId)
        }

        @Test
        @DisplayName("GIVEN invalid snippet code WHEN creating THEN returns invalid result with errors")
        fun `returns invalid result when code has syntax errors`() {
            // Given
            val owner = TestUsers.OWNER
            val snippet =
                submitSnippet {
                    code = "invalid code syntax!!!"
                    name = "Bad Snippet"
                    language = "printscript"
                }

            val errorResponse =
                ExecuteResDTO(
                    snippetId = UUID.randomUUID(),
                    outputs = emptyList(),
                    errors = listOf("Syntax error at line 1: unexpected token"),
                )

            whenever(engineService.parse(any<ExecuteReqDTO>())).thenReturn(errorResponse)

            // When
            val result = snippetService.createSnippet(snippet, owner.id)

            // Then
            assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
            val invalid = result as SnippetSubmissionResult.InvalidSnippet
            assertTrue(invalid.message.isNotEmpty())
            verify(assetService, never()).upload(anyString(), anyString(), anyString())
            verify(snippetRepository, never()).save(any<SnippetMetadata>())
        }

        @Test
        @DisplayName("GIVEN snippet with version tag WHEN creating THEN stores correct version")
        fun `stores snippet with correct version tag`() {
            // Given
            val owner = TestUsers.OWNER
            val snippet =
                submitSnippet {
                    code = "let y: string = \"hello\";"
                    name = "Versioned Snippet"
                    versionTag = "2.0"
                }

            val validResponse =
                ExecuteResDTO(
                    snippetId = UUID.randomUUID(),
                    outputs = emptyList(),
                    errors = emptyList(),
                )

            whenever(engineService.parse(any<ExecuteReqDTO>())).thenReturn(validResponse)
            whenever(assetService.upload(anyString(), anyString(), anyString())).thenReturn("Asset uploaded")
            whenever(snippetRepository.save(any<SnippetMetadata>())).thenAnswer { it.arguments[0] }
            whenever(snippetVersionRepository.save(any<SnippetVersion>())).thenAnswer { it.arguments[0] }

            // When
            val result = snippetService.createSnippet(snippet, owner.id)

            // Then
            assertTrue(result is SnippetSubmissionResult.Success)
            assertEquals("2.0", (result as SnippetSubmissionResult.Success).version)
        }

        @Test
        @DisplayName("GIVEN snippet without version tag WHEN creating THEN uses default version")
        fun `uses default version when not provided`() {
            // Given
            val owner = TestUsers.OWNER
            val snippet =
                submitSnippet {
                    code = "println(1);"
                    name = "Default Version Snippet"
                    versionTag = null
                }

            val validResponse =
                ExecuteResDTO(
                    snippetId = UUID.randomUUID(),
                    outputs = emptyList(),
                    errors = emptyList(),
                )

            whenever(engineService.parse(any<ExecuteReqDTO>())).thenReturn(validResponse)
            whenever(assetService.upload(anyString(), anyString(), anyString())).thenReturn("Asset uploaded")
            whenever(snippetRepository.save(any<SnippetMetadata>())).thenAnswer { it.arguments[0] }
            whenever(snippetVersionRepository.save(any<SnippetVersion>())).thenAnswer { it.arguments[0] }

            // When
            val result = snippetService.createSnippet(snippet, owner.id)

            // Then
            assertTrue(result is SnippetSubmissionResult.Success)
            assertEquals("1.0", (result as SnippetSubmissionResult.Success).version)
        }
    }

    @Nested
    @DisplayName("US #2 & #4: Update Snippet")
    inner class UpdateSnippetTests {
        @Test
        @DisplayName("GIVEN owner updates snippet WHEN valid code THEN updates successfully")
        fun `updates snippet when owner and valid code`() {
            // Given
            val owner = TestUsers.OWNER
            val snippetId = UUID.randomUUID()
            val existingSnippet =
                snippetMetadata {
                    id = snippetId
                    name = "Existing Snippet"
                    ownerId = owner.id
                }
            val newCode = "let updated: number = 42;"

            val validResponse =
                ExecuteResDTO(
                    snippetId = snippetId,
                    outputs = emptyList(),
                    errors = emptyList(),
                )

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(existingSnippet))
            whenever(engineService.parse(any<ExecuteReqDTO>())).thenReturn(validResponse)
            whenever(assetService.update(anyString(), anyString(), anyString())).thenReturn("Asset updated")

            // When
            val result = snippetService.updateSnippet(snippetId, newCode, owner.id)

            // Then
            assertTrue(result is SnippetSubmissionResult.Success)
            val success = result as SnippetSubmissionResult.Success
            assertEquals(snippetId, success.snippetId)
        }

        @Test
        @DisplayName("GIVEN non-owner updates snippet WHEN updating THEN returns access denied error")
        fun `returns error when non-owner tries to update`() {
            // Given
            val owner = TestUsers.OWNER
            val stranger = TestUsers.STRANGER
            val snippetId = UUID.randomUUID()
            val existingSnippet =
                snippetMetadata {
                    id = snippetId
                    ownerId = owner.id
                }

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(existingSnippet))

            // When
            val result = snippetService.updateSnippet(snippetId, "new code", stranger.id)

            // Then
            assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
            val invalid = result as SnippetSubmissionResult.InvalidSnippet
            assertTrue(invalid.message.any { it.contains("not the owner") })
            verify(engineService, never()).parse(any<ExecuteReqDTO>())
            verify(assetService, never()).update(anyString(), anyString(), anyString())
        }

        @Test
        @DisplayName("GIVEN non-existent snippet WHEN updating THEN returns not found error")
        fun `returns error when snippet not found`() {
            // Given
            val nonExistentId = UUID.randomUUID()
            whenever(snippetRepository.findById(nonExistentId)).thenReturn(Optional.empty())

            // When
            val result = snippetService.updateSnippet(nonExistentId, "code", TestUsers.OWNER.id)

            // Then
            assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
            val invalid = result as SnippetSubmissionResult.InvalidSnippet
            assertTrue(invalid.message.any { it.contains("not found") })
        }

        @Test
        @DisplayName("GIVEN invalid new code WHEN updating THEN returns validation errors")
        fun `returns error when new code is invalid`() {
            // Given
            val owner = TestUsers.OWNER
            val snippetId = UUID.randomUUID()
            val existingSnippet =
                snippetMetadata {
                    id = snippetId
                    ownerId = owner.id
                }
            val invalidCode = "this is not valid code!!!"

            val errorResponse =
                ExecuteResDTO(
                    snippetId = snippetId,
                    outputs = emptyList(),
                    errors = listOf("Parse error: invalid syntax"),
                )

            whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(existingSnippet))
            whenever(engineService.parse(any<ExecuteReqDTO>())).thenReturn(errorResponse)

            // When
            val result = snippetService.updateSnippet(snippetId, invalidCode, owner.id)

            // Then
            assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
            verify(assetService, never()).update(anyString(), anyString(), anyString())
        }
    }
}
