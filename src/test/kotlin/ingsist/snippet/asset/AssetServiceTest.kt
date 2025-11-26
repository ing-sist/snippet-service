package ingsist.snippet.asset

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
import org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI

@DisplayName("AssetService")
class AssetServiceTest {
    private lateinit var assetService: AssetService
    private lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        mockServer = MockRestServiceServer.bindTo(builder).build()
        assetService = AssetService(builder.build())
    }

    @Test
    @DisplayName("GIVEN valid content WHEN uploading asset THEN returns success message")
    fun `upload returns success message`() {
        val container = "snippets"
        val key = "test-key"
        val content = "some content"

        mockServer.expect(requestTo("/v1/asset/$container/$key"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(content))
            .andRespond(withCreatedEntity(URI.create("/v1/asset/$container/$key")).body("Success"))

        val result = assetService.upload(container, key, content)

        assertEquals("Asset uploaded successfully in $container with key $key", result)
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN valid content WHEN updating asset THEN returns success message")
    fun `update returns success message`() {
        val container = "snippets"
        val key = "test-key"
        val content = "updated content"

        mockServer.expect(requestTo("/v1/asset/$container/$key"))
            .andExpect(method(HttpMethod.PATCH))
            .andExpect(content().string(content))
            .andRespond(withCreatedEntity(URI.create("/v1/asset/$container/$key")).body("Success"))

        val result = assetService.update(container, key, content)

        assertEquals("Asset updated successfully in $container with key $key", result)
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN valid key WHEN deleting asset THEN returns success message")
    fun `delete returns success message`() {
        val container = "snippets"
        val key = "test-key"

        mockServer.expect(requestTo("/v1/asset/$container/$key"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withCreatedEntity(URI.create("/v1/asset/$container/$key")).body("Success"))

        val result = assetService.delete(container, key)

        assertEquals("Asset deleted successfully in $container with key $key", result)
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN valid key WHEN getting asset THEN returns content")
    fun `get returns content`() {
        val container = "snippets"
        val key = "test-key"
        val content = "some content"

        mockServer.expect(requestTo("/v1/asset/$container/$key"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(content, MediaType.TEXT_PLAIN))

        val result = assetService.get(container, key)

        assertEquals(content, result)
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN service error WHEN uploading asset THEN throws ExternalServiceException")
    fun `upload throws exception when service fails`() {
        val container = "snippets"
        val key = "test-key"
        val content = "some content"

        mockServer.expect(requestTo("/v1/asset/$container/$key"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows(ExternalServiceException::class.java) {
            assetService.upload(container, key, content)
        }
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN service error WHEN updating asset THEN throws ExternalServiceException")
    fun `update throws exception when service fails`() {
        val container = "snippets"
        val key = "test-key"
        val content = "updated content"

        mockServer.expect(requestTo("/v1/asset/$container/$key"))
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows(ExternalServiceException::class.java) {
            assetService.update(container, key, content)
        }
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN service error WHEN deleting asset THEN throws ExternalServiceException")
    fun `delete throws exception when service fails`() {
        val container = "snippets"
        val key = "test-key"

        mockServer.expect(requestTo("/v1/asset/$container/$key"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows(ExternalServiceException::class.java) {
            assetService.delete(container, key)
        }
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN service error WHEN getting asset THEN throws ExternalServiceException")
    fun `get throws exception when service fails`() {
        val container = "snippets"
        val key = "test-key"

        mockServer.expect(requestTo("/v1/asset/$container/$key"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertThrows(ExternalServiceException::class.java) {
            assetService.get(container, key)
        }
        mockServer.verify()
    }
}
