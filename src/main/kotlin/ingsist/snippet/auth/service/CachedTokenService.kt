package ingsist.snippet.auth.service

import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CachedTokenService(
    private val auth0TokenService: Auth0TokenService,
) {
    private var cachedToken: String? = null
    private var expiresAt: Instant = Instant.MIN

    @Synchronized
    fun getToken(): String {
        // Si no hay token o falta poco para que expire (ej. buffer de 60s)
        if (cachedToken == null || Instant.now().plusSeconds(60).isAfter(expiresAt)) {
            val response = auth0TokenService.getM2MToken()
            cachedToken = response.accessToken
            // Calculamos expiración real basada en "expires_in" (segundos)
            expiresAt = Instant.now().plusSeconds(response.expiresIn.toLong())
        }
        return cachedToken!!
    }
}
