package ingsist.snippet.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CachedTokenServiceTest {
    @Mock
    private lateinit var auth0TokenService: Auth0TokenService

    @InjectMocks
    private lateinit var cachedTokenService: CachedTokenService

    @Test
    fun `should get token from service when not cached`() {
        val tokenResponse = Auth0TokenResponse("new-token", "Bearer", 3600)
        Mockito.`when`(auth0TokenService.getM2MToken()).thenReturn(tokenResponse)

        val result = cachedTokenService.getToken()
        assertEquals("new-token", result)
        Mockito.verify(auth0TokenService, Mockito.times(1)).getM2MToken()
    }

    @Test
    fun `should return cached token when valid`() {
        val tokenResponse = Auth0TokenResponse("cached-token", "Bearer", 3600)
        Mockito.`when`(auth0TokenService.getM2MToken()).thenReturn(tokenResponse)

        // First call to populate cache
        cachedTokenService.getToken()

        // Second call should use cache
        val result = cachedTokenService.getToken()
        assertEquals("cached-token", result)
        Mockito.verify(auth0TokenService, Mockito.times(1)).getM2MToken()
    }

    @Test
    fun `should refresh token when expired`() {
        // Mock first token with short expiration (less than 60s buffer)
        val expiredTokenResponse = Auth0TokenResponse("expired-token", "Bearer", 10)
        Mockito.`when`(auth0TokenService.getM2MToken()).thenReturn(expiredTokenResponse)

        cachedTokenService.getToken()

        // Mock second token
        val newTokenResponse = Auth0TokenResponse("new-token", "Bearer", 3600)
        Mockito.`when`(auth0TokenService.getM2MToken()).thenReturn(newTokenResponse)

        val result = cachedTokenService.getToken()
        assertEquals("new-token", result)
        Mockito.verify(auth0TokenService, Mockito.times(2)).getM2MToken()
    }
}
