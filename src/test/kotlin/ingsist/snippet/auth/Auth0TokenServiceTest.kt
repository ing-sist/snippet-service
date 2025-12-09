package ingsist.snippet.auth

import ingsist.snippet.shared.exception.ExternalServiceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RestClientTest(Auth0TokenService::class)
@TestPropertySource(
    properties = [
        "auth0.m2m.client-id=test-client-id",
        "auth0.m2m.client-secret=test-client-secret",
        "auth0.audience=test-audience",
        "auth0.m2m.token-url=http://auth0.com/oauth/token",
    ],
)
class Auth0TokenServiceTest {
    @Autowired
    private lateinit var auth0TokenService: Auth0TokenService

    @Autowired
    private lateinit var server: MockRestServiceServer

    @Test
    fun `should get M2M token successfully`() {
        val responseJson = """
            {
                "access_token": "test-token",
                "token_type": "Bearer",
                "expires_in": 3600
            }
        """

        server.expect(requestTo("http://auth0.com/oauth/token"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON))

        val result = auth0TokenService.getM2MToken()
        assertEquals("test-token", result.accessToken)
        assertEquals("Bearer", result.tokenType)
        assertEquals(3600, result.expiresIn)
    }

    @Test
    fun `should throw exception when get M2M token fails`() {
        server.expect(requestTo("http://auth0.com/oauth/token"))
            .andRespond(withServerError())

        assertThrows(ExternalServiceException::class.java) {
            auth0TokenService.getM2MToken()
        }
    }

    @Test
    fun `should throw exception when get M2M token returns empty body`() {
        server.expect(requestTo("http://auth0.com/oauth/token"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        assertThrows(ExternalServiceException::class.java) {
            auth0TokenService.getM2MToken()
        }
    }
}
