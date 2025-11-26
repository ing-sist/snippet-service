package ingsist.snippet.controller

import com.fasterxml.jackson.databind.ObjectMapper
import ingsist.snippet.config.TestSecurityConfig
import ingsist.snippet.exception.GlobalExceptionHandler
import ingsist.snippet.exception.SnippetAccessDeniedException
import ingsist.snippet.exception.SnippetNotFoundException
import ingsist.snippet.service.SnippetService
import ingsist.snippet.test.TestUsers
import ingsist.snippet.test.shareSnippet
import ingsist.snippet.test.snippetResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(SnippetController::class)
@Import(TestSecurityConfig::class, GlobalExceptionHandler::class)
@DisplayName("SnippetController")
class SnippetControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var snippetService: SnippetService

    private val user = TestUsers.OWNER

    @Nested
    @DisplayName("US #5: GET /snippets - List All Snippets")
    inner class GetAllSnippetsTests {
        @Test
        @DisplayName("GIVEN authenticated user WHEN listing snippets THEN returns 200 with snippets list")
        fun `returns snippets list successfully`() {
            // Given
            val snippets =
                listOf(
                    snippetResponse { name = "First Snippet" },
                    snippetResponse { name = "Second Snippet" },
                )

            whenever(snippetService.getAllSnippets(eq(user.id), eq(0), eq(10), any()))
                .thenReturn(snippets)

            // When & Then
            mockMvc.perform(
                get("/snippets")
                    .with(jwt().jwt { it.subject(user.id) })
                    .param("page", "0")
                    .param("size", "10"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("First Snippet"))
                .andExpect(jsonPath("$[1].name").value("Second Snippet"))
        }

        @Test
        @DisplayName("GIVEN authenticated user with no snippets WHEN listing snippets THEN returns 200 with empty list")
        fun `returns empty list when no snippets`() {
            // Given
            whenever(snippetService.getAllSnippets(eq(user.id), eq(0), eq(10), any()))
                .thenReturn(emptyList())

            // When & Then
            mockMvc.perform(
                get("/snippets")
                    .with(jwt().jwt { it.subject(user.id) }),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }

        @Test
        @DisplayName("GIVEN no authentication WHEN listing snippets THEN returns 401 Unauthorized")
        fun `returns 401 without authentication`() {
            mockMvc.perform(get("/snippets"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        @DisplayName("GIVEN custom pagination WHEN listing snippets THEN respects pagination parameters")
        fun `respects custom pagination`() {
            // Given
            val snippets = listOf(snippetResponse { name = "Paginated Snippet" })
            whenever(snippetService.getAllSnippets(eq(user.id), eq(2), eq(5), any()))
                .thenReturn(snippets)

            // When & Then
            mockMvc.perform(
                get("/snippets")
                    .with(jwt().jwt { it.subject(user.id) })
                    .param("page", "2")
                    .param("size", "5"),
            )
                .andExpect(status().isOk)
        }
    }

    @Nested
    @DisplayName("US #6: GET /snippets/{id} - Get Snippet Details")
    inner class GetSnippetByIdTests {
        @Test
        @DisplayName("GIVEN existing snippet WHEN getting by ID THEN returns 200 with snippet details")
        fun `returns snippet details successfully`() {
            // Given
            val snippetId = UUID.randomUUID()
            val snippet =
                snippetResponse {
                    id = snippetId
                    name = "My Snippet"
                    language = "printscript"
                    description = "A test snippet"
                    ownerId = user.id
                    version = "1.0"
                }

            whenever(snippetService.getSnippetById(snippetId)).thenReturn(snippet)

            // When & Then
            mockMvc.perform(
                get("/snippets/{id}", snippetId)
                    .with(jwt().jwt { it.subject(user.id) }),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(snippetId.toString()))
                .andExpect(jsonPath("$.name").value("My Snippet"))
                .andExpect(jsonPath("$.language").value("printscript"))
                .andExpect(jsonPath("$.description").value("A test snippet"))
                .andExpect(jsonPath("$.ownerId").value(user.id))
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.compliance").value("pending"))
        }

        @Test
        @DisplayName("GIVEN non-existing snippet WHEN getting by ID THEN returns 404 Not Found")
        fun `returns 404 when snippet not found`() {
            // Given
            val snippetId = UUID.randomUUID()
            whenever(snippetService.getSnippetById(snippetId))
                .thenThrow(SnippetNotFoundException("Snippet with id $snippetId not found"))

            // When & Then
            mockMvc.perform(
                get("/snippets/{id}", snippetId)
                    .with(jwt().jwt { it.subject(user.id) }),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Snippet with id $snippetId not found"))
        }

        @Test
        @DisplayName("GIVEN no authentication WHEN getting snippet THEN returns 401 Unauthorized")
        fun `returns 401 without authentication`() {
            val snippetId = UUID.randomUUID()
            mockMvc.perform(get("/snippets/{id}", snippetId))
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("US #7: POST /snippets/{id}/share - Share Snippet")
    inner class ShareSnippetTests {
        @Test
        @DisplayName("GIVEN owner shares snippet WHEN sharing THEN returns 200 OK")
        fun `shares snippet successfully`() {
            // Given
            val snippetId = UUID.randomUUID()
            val targetUser = TestUsers.COLLABORATOR
            val shareDTO = shareSnippet { targetUserId = targetUser.id }

            doNothing().whenever(snippetService).shareSnippet(
                eq(snippetId),
                eq(targetUser.id),
                eq(user.id),
                any(),
            )

            // When & Then
            mockMvc.perform(
                post("/snippets/{id}/share", snippetId)
                    .with(jwt().jwt { it.subject(user.id) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shareDTO)),
            )
                .andExpect(status().isOk)
        }

        @Test
        @DisplayName("GIVEN non-existing snippet WHEN sharing THEN returns 404 Not Found")
        fun `returns 404 when snippet not found`() {
            // Given
            val snippetId = UUID.randomUUID()
            val shareDTO = shareSnippet { targetUserId = "target-user" }

            doThrow(SnippetNotFoundException("Snippet with id $snippetId not found"))
                .whenever(snippetService).shareSnippet(
                    eq(snippetId),
                    any(),
                    eq(user.id),
                    any(),
                )

            // When & Then
            mockMvc.perform(
                post("/snippets/{id}/share", snippetId)
                    .with(jwt().jwt { it.subject(user.id) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shareDTO)),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("Not Found"))
        }

        @Test
        @DisplayName("GIVEN non-owner shares snippet WHEN sharing THEN returns 403 Forbidden")
        fun `returns 403 when user is not owner`() {
            // Given
            val snippetId = UUID.randomUUID()
            val shareDTO = shareSnippet { targetUserId = "target-user" }

            doThrow(SnippetAccessDeniedException("You don't have permission to share this snippet"))
                .whenever(snippetService).shareSnippet(
                    eq(snippetId),
                    any(),
                    eq(user.id),
                    any(),
                )

            // When & Then
            mockMvc.perform(
                post("/snippets/{id}/share", snippetId)
                    .with(jwt().jwt { it.subject(user.id) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shareDTO)),
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error").value("Forbidden"))
        }

        @Test
        @DisplayName("GIVEN no authentication WHEN sharing snippet THEN returns 401 Unauthorized")
        fun `returns 401 without authentication`() {
            val snippetId = UUID.randomUUID()
            val shareDTO = shareSnippet { targetUserId = "target-user" }

            mockMvc.perform(
                post("/snippets/{id}/share", snippetId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shareDTO)),
            )
                .andExpect(status().isUnauthorized)
        }
    }
}
