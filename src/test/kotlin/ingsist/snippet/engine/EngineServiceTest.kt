package ingsist.snippet.engine

import com.fasterxml.jackson.databind.ObjectMapper
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.ExecuteResDTO
import ingsist.snippet.exception.ExternalServiceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.util.UUID

@DisplayName("EngineService")
class EngineServiceTest {
    private lateinit var engineService: EngineService
    private lateinit var mockServer: MockRestServiceServer
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        mockServer = MockRestServiceServer.bindTo(builder).build()
        engineService = EngineService(builder.build())
    }

    @Test
    @DisplayName("GIVEN valid snippet WHEN parsing THEN returns execution result")
    fun `parse returns execution result`() {
        val snippetId = UUID.randomUUID()
        val request =
            ExecuteReqDTO(
                code = "print('hello')",
                language = "printscript",
                version = "1.0",
                snippetId = snippetId,
            )
        val response =
            ExecuteResDTO(
                snippetId = snippetId,
                outputs = listOf("hello"),
                errors = emptyList(),
            )

        mockServer.expect(requestTo("/v1/engine/parse"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(objectMapper.writeValueAsString(request)))
            .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON))

        val result = engineService.parse(request)

        assertEquals(response.outputs, result.outputs)
        assertEquals(response.errors, result.errors)
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN service error WHEN parsing THEN throws ExternalServiceException")
    fun `parse throws exception when service fails`() {
        val snippetId = UUID.randomUUID()
        val request =
            ExecuteReqDTO(
                code = "print('hello')",
                language = "printscript",
                version = "1.0",
                snippetId = snippetId,
            )

        mockServer.expect(requestTo("/v1/engine/parse"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows(ExternalServiceException::class.java) {
            engineService.parse(request)
        }
        mockServer.verify()
    }
}
