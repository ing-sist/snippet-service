package ingsist.snippet.client

import com.fasterxml.jackson.databind.ObjectMapper
import ingsist.snippet.exception.ExternalServiceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.util.UUID

@DisplayName("AuthClient")
class AuthClientTest {
    private lateinit var authClient: AuthClient
    private lateinit var mockServer: MockRestServiceServer
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        mockServer = MockRestServiceServer.bindTo(builder).build()
        authClient = AuthClient(builder.build())
    }

    @Test
    @DisplayName("GIVEN valid user and token WHEN getting permissions THEN returns list of snippet IDs")
    fun `getUserPermissions returns list of snippet IDs when successful`() {
        val userId = "user1"
        val token = "token123"
        val snippetId = UUID.randomUUID()
        val permissions =
            listOf(
                SnippetPermissionDto(snippetId.toString(), userId, "READ"),
            )

        mockServer.expect(requestTo("/users/$userId/permissions"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer $token"))
            .andRespond(withSuccess(objectMapper.writeValueAsString(permissions), MediaType.APPLICATION_JSON))

        val result = authClient.getUserPermissions(userId, token)

        assertEquals(1, result.size)
        assertEquals(snippetId, result[0])
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN service error WHEN getting permissions THEN returns empty list")
    fun `getUserPermissions returns empty list when error occurs`() {
        val userId = "user1"
        val token = "token123"

        mockServer.expect(requestTo("/users/$userId/permissions"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        val result = authClient.getUserPermissions(userId, token)

        assertTrue(result.isEmpty())
        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN valid share request WHEN sharing snippet THEN sends correct request")
    fun `shareSnippet sends correct request`() {
        val snippetId = UUID.randomUUID()
        val targetUserId = "targetUser"
        val token = "token123"
        val request = GrantPermissionRequest(userId = targetUserId)

        mockServer.expect(requestTo("/snippets/$snippetId/permissions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer $token"))
            .andExpect(content().json(objectMapper.writeValueAsString(request)))
            .andRespond(withSuccess())

        authClient.shareSnippet(snippetId, targetUserId, token)

        mockServer.verify()
    }

    @Test
    @DisplayName("GIVEN service error WHEN sharing snippet THEN throws ExternalServiceException")
    fun `shareSnippet throws exception when service fails`() {
        val snippetId = UUID.randomUUID()
        val targetUserId = "targetUser"
        val token = "token123"
        val request = GrantPermissionRequest(userId = targetUserId)

        mockServer.expect(requestTo("/snippets/$snippetId/permissions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows(ExternalServiceException::class.java) {
            authClient.shareSnippet(snippetId, targetUserId, token)
        }
        mockServer.verify()
    }
}
