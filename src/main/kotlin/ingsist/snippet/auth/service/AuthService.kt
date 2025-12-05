package ingsist.snippet.auth.service

import ingsist.snippet.runner.snippet.dtos.PermissionDTO
import ingsist.snippet.runner.user.dtos.UserResponseDTO
import ingsist.snippet.shared.exception.ExternalServiceException
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

@Component
class AuthService(
    private val restClientBuilder: RestClient.Builder,
    @Value("\${external.auth.url}") private val authServiceUrl: String,
) {
    private val client = restClientBuilder.baseUrl(authServiceUrl).build()

    // US #7: Buscar usuarios (Propaga token de usuario)
    fun getUsers(
        email: String,
        token: String,
    ): List<UserResponseDTO> {
        return try {
            client.get()
                .uri("/users?email={email}", email)
                .header("Authorization", token)
                .retrieve()
                .body(object : ParameterizedTypeReference<List<UserResponseDTO>>() {}) ?: emptyList()
        } catch (e: RestClientException) {
            throw ExternalServiceException("Error getting users: ${e.message}", e)
        }
    }

    // US #7: Compartir (Propaga token de usuario)
    fun grantPermission(
        dto: PermissionDTO,
        token: String,
    ) {
        try {
            client.post()
                .uri("/permissions")
                .header("Authorization", token)
                .body(dto)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            throw ExternalServiceException("Error granting permission: ${e.message}", e)
        }
    }

    // US #5: Obtener snippets compartidos conmigo
    fun getSharedSnippets(
        userId: String,
        token: String,
    ): List<PermissionDTO> {
        return try {
            client.get()
                .uri("/permissions/user/{userId}", userId)
                .header("Authorization", token)
                .retrieve()
                .body(object : ParameterizedTypeReference<List<PermissionDTO>>() {}) ?: emptyList()
        } catch (e: RestClientException) {
            throw ExternalServiceException("Error getting shared snippets: ${e.message}", e)
        }
    }

    // US #6: Validar acceso (Propaga token de usuario)
    fun hasPermission(
        snippetId: UUID,
        permission: String,
        token: String,
    ): Boolean {
        return try {
            // Asumimos que tu Auth Service usa el token para identificar al usuario que pregunta
            val response =
                client.get()
                    .uri("/permissions/snippet/{snippetId}?permission={permission}", snippetId, permission)
                    .header("Authorization", token)
                    .retrieve()
                    .body(Boolean::class.java)
            response ?: false
        } catch (e: RestClientException) {
            throw ExternalServiceException("Error checking permission: ${e.message}", e)
        }
    }

    fun deleteSnippetPermissions(
        snippetId: UUID,
        token: String,
    ) {
        try {
            client.delete()
                .uri("/snippet/{snippetId}", snippetId)
                .header("Authorization", token)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            throw ExternalServiceException("Error deleting snippet permissions: ${e.message}", e)
        }
    }
}
