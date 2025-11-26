package ingsist.snippet.controller

import ingsist.snippet.config.TestSecurityConfig
import ingsist.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.exception.GlobalExceptionHandler
import ingsist.snippet.service.SnippetService
import ingsist.snippet.test.TestUsers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Controller tests for Create & Update operations (US #1-4).
 */
@WebMvcTest(SnippetController::class)
@Import(TestSecurityConfig::class, GlobalExceptionHandler::class)
@DisplayName("SnippetController - Create & Update Operations")
class SnippetControllerCreateUpdateTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var snippetService: SnippetService

    private val user = TestUsers.OWNER

    @Nested
    @DisplayName("US #1: POST /snippets/upload-from-file - Upload Snippet from File")
    inner class UploadFromFileEndpoint {
        @Test
        @DisplayName("GIVEN valid file WHEN uploading snippet THEN returns 201 Created")
        fun `uploads snippet from file successfully`() {
            // Given
            val snippetId = UUID.randomUUID()
            val file =
                MockMultipartFile(
                    "file",
                    "snippet.ps",
                    "text/plain",
                    "let x: number = 1;".toByteArray(),
                )

            val successResult =
                SnippetSubmissionResult.Success(
                    snippetId = snippetId,
                    name = "My Snippet",
                    language = "printscript",
                    version = "1.0",
                )

            whenever(snippetService.createSnippet(any(), eq(user.id))).thenReturn(successResult)

            // When & Then
            mockMvc.perform(
                multipart("/snippets/upload-from-file")
                    .file(file)
                    .param("name", "My Snippet")
                    .param("language", "printscript")
                    .param("version", "1.1")
                    .param("description", "Test snippet")
                    .with(jwt().jwt { it.subject(user.id) }),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.snippetId").value(snippetId.toString()))
                .andExpect(jsonPath("$.name").value("My Snippet"))
        }

        @Test
        @DisplayName("GIVEN invalid code WHEN uploading THEN returns 422 Unprocessable Entity")
        fun `returns 422 when code is invalid`() {
            // Given
            val file =
                MockMultipartFile(
                    "file",
                    "bad.ps",
                    "text/plain",
                    "invalid!!!".toByteArray(),
                )

            val invalidResult =
                SnippetSubmissionResult.InvalidSnippet(
                    message = listOf("Syntax error at line 1"),
                )

            whenever(snippetService.createSnippet(any(), eq(user.id))).thenReturn(invalidResult)

            // When & Then
            mockMvc.perform(
                multipart("/snippets/upload-from-file")
                    .file(file)
                    .param("name", "Bad Snippet")
                    .param("language", "printscript")
                    .param("version", "1.1")
                    .param("description", "Test")
                    .with(jwt().jwt { it.subject(user.id) }),
            )
                .andExpect(status().isUnprocessableEntity)
        }

        @Test
        @DisplayName("GIVEN no authentication WHEN uploading THEN returns 401 Unauthorized")
        fun `returns 401 without authentication`() {
            val file =
                MockMultipartFile(
                    "file",
                    "snippet.ps",
                    "text/plain",
                    "code".toByteArray(),
                )

            mockMvc.perform(
                multipart("/snippets/upload-from-file")
                    .file(file)
                    .param("name", "Snippet")
                    .param("language", "printscript")
                    .param("version", "1.1")
                    .param("description", "Test"),
            )
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("US #3: POST /snippets/upload-inline - Create Snippet from Editor")
    inner class UploadInlineEndpoint {
        @Test
        @DisplayName("GIVEN valid inline code WHEN creating snippet THEN returns 201 Created")
        fun `creates snippet from inline code successfully`() {
            // Given
            val snippetId = UUID.randomUUID()
            val code = "let message: string = \"Hello World\";"

            val successResult =
                SnippetSubmissionResult.Success(
                    snippetId = snippetId,
                    name = "Hello Snippet",
                    language = "printscript",
                    version = "1.0",
                )

            whenever(snippetService.createSnippet(any(), eq(user.id))).thenReturn(successResult)

            // When & Then
            mockMvc.perform(
                post("/snippets/upload-inline")
                    .with(jwt().jwt { it.subject(user.id) })
                    .param("code", code)
                    .param("name", "Hello Snippet")
                    .param("language", "printscript")
                    .param("version", "1.1")
                    .param("description", "A hello world snippet"),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.snippetId").value(snippetId.toString()))
        }

        @Test
        @DisplayName("GIVEN invalid code WHEN creating inline snippet THEN returns 422")
        fun `returns 422 when inline code is invalid`() {
            val invalidResult =
                SnippetSubmissionResult.InvalidSnippet(
                    message = listOf("Unexpected token 'bad'"),
                )

            whenever(snippetService.createSnippet(any(), eq(user.id))).thenReturn(invalidResult)

            // When & Then
            mockMvc.perform(
                post("/snippets/upload-inline")
                    .with(jwt().jwt { it.subject(user.id) })
                    .param("code", "bad code here!!!")
                    .param("name", "Bad Snippet")
                    .param("language", "printscript")
                    .param("version", "1.1")
                    .param("description", "Test"),
            )
                .andExpect(status().isUnprocessableEntity)
        }
    }

    @Nested
    @DisplayName("US #2 & #4: PUT /snippets/{id} - Update Snippet")
    inner class UpdateSnippetEndpoint {
        @Test
        @DisplayName("GIVEN owner updates snippet WHEN valid code THEN returns 200 OK")
        fun `updates snippet successfully`() {
            // Given
            val snippetId = UUID.randomUUID()
            val newCode = "let updated: number = 99;"

            val successResult =
                SnippetSubmissionResult.Success(
                    snippetId = snippetId,
                    name = "Updated Snippet",
                    language = "printscript",
                    version = "1.0",
                )

            whenever(snippetService.updateSnippet(eq(snippetId), eq(newCode), eq(user.id)))
                .thenReturn(successResult)

            // When & Then
            mockMvc.perform(
                put("/snippets/{id}", snippetId)
                    .with(jwt().jwt { it.subject(user.id) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newCode),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.snippetId").value(snippetId.toString()))
        }

        @Test
        @DisplayName("GIVEN non-owner updates snippet WHEN updating THEN returns 422")
        fun `returns 422 when non-owner updates`() {
            // Given
            val snippetId = UUID.randomUUID()
            val invalidResult =
                SnippetSubmissionResult.InvalidSnippet(
                    message = listOf("You are not the owner of this snippet"),
                )

            whenever(snippetService.updateSnippet(eq(snippetId), any(), eq(user.id)))
                .thenReturn(invalidResult)

            // When & Then
            mockMvc.perform(
                put("/snippets/{id}", snippetId)
                    .with(jwt().jwt { it.subject(user.id) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("new code"),
            )
                .andExpect(status().isUnprocessableEntity)
        }

        @Test
        @DisplayName("GIVEN invalid code WHEN updating THEN returns 422 Unprocessable Entity")
        fun `returns 422 when code is invalid`() {
            // Given
            val snippetId = UUID.randomUUID()
            val invalidResult =
                SnippetSubmissionResult.InvalidSnippet(
                    message = listOf("Parse error: invalid syntax"),
                )

            whenever(snippetService.updateSnippet(eq(snippetId), any(), eq(user.id)))
                .thenReturn(invalidResult)

            // When & Then
            mockMvc.perform(
                put("/snippets/{id}", snippetId)
                    .with(jwt().jwt { it.subject(user.id) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("bad code!!!"),
            )
                .andExpect(status().isUnprocessableEntity)
        }

        @Test
        @DisplayName("GIVEN no authentication WHEN updating THEN returns 401 Unauthorized")
        fun `returns 401 without authentication`() {
            val snippetId = UUID.randomUUID()

            mockMvc.perform(
                put("/snippets/{id}", snippetId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("code"),
            )
                .andExpect(status().isUnauthorized)
        }
    }
}
