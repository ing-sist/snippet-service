package ingsist.snippet.test.controller

import com.fasterxml.jackson.databind.ObjectMapper
import ingsist.snippet.test.model.dto.CreateTestRequest
import ingsist.snippet.test.model.dto.RunStatus
import ingsist.snippet.test.model.dto.RunTestResponse
import ingsist.snippet.test.model.dto.SnippetTestResponse
import ingsist.snippet.test.service.SnippetTestService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(SnippetTestController::class)
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class SnippetTestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtDecoder: JwtDecoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var snippetTestService: SnippetTestService

    @Test
    fun `should create test`() {
        val snippetId = UUID.randomUUID()
        val request = CreateTestRequest("Test 1", listOf("input"), listOf("output"))
        val response = SnippetTestResponse(UUID.randomUUID(), "Test 1", listOf("input"), listOf("output"), "1.0")

        Mockito.`when`(
            snippetTestService.createTest(
                any(UUID::class.java) ?: UUID.randomUUID(),
                any(String::class.java) ?: "",
                any(CreateTestRequest::class.java) ?: request,
            ),
        )
            .thenReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/snippets/$snippetId/tests")
                .with(jwt().jwt { it.subject("user-1") })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `should list tests`() {
        val snippetId = UUID.randomUUID()
        val response =
            listOf(
                SnippetTestResponse(
                    UUID.randomUUID(),
                    "Test 1",
                    listOf("input"),
                    listOf("output"),
                    "1.0",
                ),
            )

        Mockito.`when`(
            snippetTestService.listTests(
                any(UUID::class.java) ?: UUID.randomUUID(),
                any(String::class.java) ?: "",
                any(String::class.java) ?: "",
            ),
        )
            .thenReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/snippets/$snippetId/tests")
                .with(jwt().jwt { it.subject("user-1") }),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete test`() {
        val snippetId = UUID.randomUUID()
        val testId = UUID.randomUUID()

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/snippets/$snippetId/tests/$testId")
                .with(jwt().jwt { it.subject("user-1") }),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should run test`() {
        val snippetId = UUID.randomUUID()
        val testId = UUID.randomUUID()
        val response = RunTestResponse(RunStatus.SUCCESS, listOf("output"), emptyList(), emptyList(), "1.0")

        Mockito.`when`(
            snippetTestService.runTest(
                any(UUID::class.java) ?: UUID.randomUUID(),
                any(UUID::class.java) ?: UUID.randomUUID(),
                any(String::class.java) ?: "",
                any(String::class.java) ?: "",
            ),
        )
            .thenReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/snippets/$snippetId/tests/$testId/run")
                .with(jwt().jwt { it.subject("user-1") }),
        )
            .andExpect(status().isOk)
    }
}
