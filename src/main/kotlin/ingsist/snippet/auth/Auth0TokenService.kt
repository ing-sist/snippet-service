package ingsist.snippet.auth

import com.fasterxml.jackson.annotation.JsonProperty
import ingsist.snippet.shared.exception.ExternalServiceException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

data class Auth0TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Int,
)

@Service
class Auth0TokenService(
    restClientBuilder: RestClient.Builder,
    @Value("\${auth0.m2m.client-id}") private val clientId: String,
    @Value("\${auth0.m2m.client-secret}") private val clientSecret: String,
    @Value("\${auth0.audience}") private val audience: String,
    @Value("\${auth0.m2m.token-url}") private val tokenUrl: String,
) {
    private val restClient = restClientBuilder.build()

    fun getM2MToken(): Auth0TokenResponse {
        val requestBody =
            mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "audience" to audience,
                "grant_type" to "client_credentials",
            )

        try {
            return restClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Auth0TokenResponse::class.java)
                ?: throw ExternalServiceException("Failed to fetch M2M Token from Auth0")
        } catch (e: RestClientException) {
            throw ExternalServiceException("Failed to fetch M2M Token from Auth0: ${e.message}", e)
        }
    }
}
