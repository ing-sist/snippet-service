package ingsist.snippet.engine

import ingsist.snippet.runner.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.runner.snippet.dtos.ValidateReqDto
import ingsist.snippet.shared.exception.ExternalServiceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.util.UUID

@RestClientTest(EngineService::class)
class EngineServiceTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun restClient(builder: RestClient.Builder): RestClient {
            return builder.build()
        }
    }

    @Autowired
    private lateinit var engineService: EngineService

    @Autowired
    private lateinit var server: MockRestServiceServer

    @Test
    fun `should parse snippet successfully`() {
        val req = ValidateReqDto(UUID.randomUUID(), "code", "1.0", "printscript", "key")
        val responseJson = """
            {
                "snippetId": "${req.snippetId}",
                "error": []
            }
        """

        server.expect(requestTo("/engine/validate"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val result = engineService.parse(req)
        assertEquals(req.snippetId, result.snippetId)
        assertEquals(0, result.error.size)
    }

    @Test
    fun `should throw exception when parse fails`() {
        val req = ValidateReqDto(UUID.randomUUID(), "code", "1.0", "printscript", "key")

        server.expect(requestTo("/engine/validate"))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            engineService.parse(req)
        }
    }

    @Test
    fun `should execute snippet successfully`() {
        val req = ExecuteReqDTO(UUID.randomUUID(), "key", mutableListOf(), "1.0", "printscript")
        val responseJson = """
            {
                "snippetId": "${req.snippetId}",
                "outputs": ["Hello"],
                "errors": []
            }
        """

        server.expect(requestTo("/engine/execute"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val result = engineService.execute(req)
        assertEquals(req.snippetId, result.snippetId)
        assertEquals("Hello", result.outputs[0])
    }

    @Test
    fun `should get snippet content`() {
        val assetKey = "some-key"
        val content = "println('hello')"

        server.expect(requestTo("/engine/code/$assetKey"))
            .andRespond(withSuccess(content, MediaType.TEXT_PLAIN))

        val result = engineService.getSnippetContent(assetKey)
        assertEquals(content, result)
    }

    @Test
    fun `should delete snippet`() {
        val assetKey = "some-key"

        server.expect(requestTo("/engine/code/$assetKey"))
            .andRespond(withSuccess())

        engineService.deleteSnippet(assetKey)
    }

    @Test
    fun `should get languages`() {
        val responseJson = """
            [
                {
                    "name": "printscript",
                    "version": ["1.0", "1.1"],
                    "extension": "ps"
                }
            ]
        """

        server.expect(requestTo("/engine/languages"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val result = engineService.getLanguages()
        assertEquals(1, result.size)
        assertEquals("printscript", result[0].name)
    }

    @Test
    fun `should throw exception when execute fails`() {
        val req = ExecuteReqDTO(UUID.randomUUID(), "key", mutableListOf(), "1.0", "printscript")

        server.expect(requestTo("/engine/execute"))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            engineService.execute(req)
        }
    }

    @Test
    fun `should throw exception when get snippet content fails`() {
        val assetKey = "some-key"

        server.expect(requestTo("/engine/code/$assetKey"))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            engineService.getSnippetContent(assetKey)
        }
    }

    @Test
    fun `should throw exception when delete snippet fails`() {
        val assetKey = "some-key"

        server.expect(requestTo("/engine/code/$assetKey"))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            engineService.deleteSnippet(assetKey)
        }
    }

    @Test
    fun `should throw exception when get languages fails`() {
        server.expect(requestTo("/engine/languages"))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            engineService.getLanguages()
        }
    }

    @Test
    fun `should throw exception when parse returns empty body`() {
        val req = ValidateReqDto(UUID.randomUUID(), "code", "1.0", "printscript", "key")

        server.expect(requestTo("/engine/validate"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        assertThrows(ExternalServiceException::class.java) {
            engineService.parse(req)
        }
    }

    @Test
    fun `should throw exception when execute returns empty body`() {
        val req = ExecuteReqDTO(UUID.randomUUID(), "key", mutableListOf(), "1.0", "printscript")

        server.expect(requestTo("/engine/execute"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        assertThrows(ExternalServiceException::class.java) {
            engineService.execute(req)
        }
    }

    @Test
    fun `should return empty string when get snippet content returns empty body`() {
        val assetKey = "some-key"

        server.expect(requestTo("/engine/code/$assetKey"))
            .andRespond(withSuccess("", MediaType.TEXT_PLAIN))

        val result = engineService.getSnippetContent(assetKey)
        assertEquals("", result)
    }

    @Test
    fun `should return empty list when get languages returns empty body`() {
        server.expect(requestTo("/engine/languages"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        val result = engineService.getLanguages()
        assertEquals(0, result.size)
    }
}
