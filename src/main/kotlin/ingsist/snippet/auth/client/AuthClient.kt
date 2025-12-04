package ingsist.snippet.auth.client

import ingsist.snippet.shared.exception.ExternalServiceException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

// DTO interno para mapear la respuesta del Auth Service
data class SnippetPermissionDto(
    val snippetId: String,
    val userId: String,
    val permission: String,
)

data class GrantPermissionRequest(
    val userId: String,
    val permission: String = "READ",
)

@Component
class AuthClient(
    @Qualifier("authRestClient") private val restClient: RestClient,
) {
    // Obtiene los permisos que tiene un usuario (US #5)
    // GET /users/{userId}/permissions
    fun getUserPermissions(
        userId: String,
        token: String,
    ): List<UUID> {
        return try {
            val permissions =
                restClient.get()
                    .uri("/users/{userId}/permissions", userId)
                    .header("Authorization", "Bearer $token") // Pasamos el token del usuario
                    .retrieve()
                    .body(Array<SnippetPermissionDto>::class.java)

            permissions?.map { UUID.fromString(it.snippetId) } ?: emptyList()
        } catch (e: RestClientException) {
            // If auth service is unavailable or user not found, return empty permissions
            println("Failed to get user permissions: ${e.message}")
            emptyList()
        }
    }

    // Comparte un snippet (US #7)
    // POST /snippets/{snippetId}/permissions
    fun shareSnippet(
        snippetId: UUID,
        targetUserId: String,
        token: String,
    ) {
        val request = GrantPermissionRequest(userId = targetUserId)

        restClient.post()
            .uri("/snippets/{snippetId}/permissions", snippetId)
            .header("Authorization", "Bearer $token")
            .body(request)
            .retrieve()
            .onStatus({ status -> status.isError }) { _, response ->
                throw ExternalServiceException("Share snippet failed with status code: ${response.statusCode}")
            }
            .toBodilessEntity()
    }
}
