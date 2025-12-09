package ingsist.snippet.auth.service

import ingsist.snippet.runner.snippet.dtos.PermissionDTO
import ingsist.snippet.shared.exception.ExternalServiceException
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.util.UUID

@RestClientTest(AuthService::class)
@TestPropertySource(properties = ["external.auth.url=http://localhost:8080"])
class AuthServiceTest {
    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var server: MockRestServiceServer

    @Test
    fun `should get users`() {
        val email = "test@example.com"
        val responseJson = """
            [
                {"id": "user1", "email": "test@example.com", "username": "user1"}
            ]
        """

        server.expect(requestTo(containsString("/users")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val result = authService.getUsers(email, "token")
        assertEquals(1, result.size)
        assertEquals("user1", result[0].id)
    }

    @Test
    fun `should grant permission`() {
        val dto = PermissionDTO("user1", UUID.randomUUID(), "READ")

        server.expect(requestTo(containsString("/permissions")))
            .andRespond(withSuccess())

        authService.grantPermission(dto, "token")
    }

    @Test
    fun `should check permission`() {
        val snippetId = UUID.randomUUID()

        server.expect(requestTo(containsString("/permissions/snippet")))
            .andRespond(withSuccess("true", MediaType.APPLICATION_JSON))

        val result = authService.hasPermission(snippetId, "READ", "token")
        assertTrue(result)
    }

    @Test
    fun `should get shared snippets`() {
        val userId = "user1"
        val responseJson = """
            [
                {"userId": "user1", "snippetId": "${UUID.randomUUID()}", "permission": "READ"}
            ]
        """

        server.expect(requestTo(containsString("/permissions/user")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val result = authService.getSharedSnippets(userId, "token")
        assertEquals(1, result.size)
    }

    @Test
    fun `should delete snippet permissions`() {
        val snippetId = UUID.randomUUID()

        server.expect(requestTo(containsString("/permissions/snippet")))
            .andRespond(withSuccess())

        authService.deleteSnippetPermissions(snippetId, "token")
    }

    @Test
    fun `should throw exception when get users fails`() {
        val email = "test@example.com"

        server.expect(requestTo(containsString("/users")))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            authService.getUsers(email, "token")
        }
    }

    @Test
    fun `should throw exception when grant permission fails`() {
        val dto = PermissionDTO("user1", UUID.randomUUID(), "READ")

        server.expect(requestTo(containsString("/permissions")))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            authService.grantPermission(dto, "token")
        }
    }

    @Test
    fun `should throw exception when check permission fails`() {
        val snippetId = UUID.randomUUID()

        server.expect(requestTo(containsString("/permissions/snippet")))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            authService.hasPermission(snippetId, "READ", "token")
        }
    }

    @Test
    fun `should throw exception when get shared snippets fails`() {
        val userId = "user1"

        server.expect(requestTo(containsString("/permissions/user")))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            authService.getSharedSnippets(userId, "token")
        }
    }

    @Test
    fun `should throw exception when delete snippet permissions fails`() {
        val snippetId = UUID.randomUUID()

        server.expect(requestTo(containsString("/permissions/snippet")))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            authService.deleteSnippetPermissions(snippetId, "token")
        }
    }

    @Test
    fun `should return empty list when get users returns empty body`() {
        val email = "test@example.com"

        server.expect(requestTo(containsString("/users")))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        val result = authService.getUsers(email, "token")
        assertEquals(0, result.size)
    }

    @Test
    fun `should return empty list when get shared snippets returns empty body`() {
        val userId = "user1"

        server.expect(requestTo(containsString("/permissions/user")))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        val result = authService.getSharedSnippets(userId, "token")
        assertEquals(0, result.size)
    }

    @Test
    fun `should return false when check permission returns empty body`() {
        val snippetId = UUID.randomUUID()

        server.expect(requestTo(containsString("/permissions/snippet")))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        val result = authService.hasPermission(snippetId, "READ", "token")
        assertEquals(false, result)
    }
}
