package ing.sist.snippet.client

import ing.sist.snippet.client.dto.AuthorizedSnippetsDto
import ing.sist.snippet.client.dto.GrantPermissionDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AuthServiceClient(
    @Value("\${service.auth.url}") private val authServiceUrl: String,
    restTemplateBuilder: RestTemplateBuilder,
) {
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    // Llama a tu auth-service para obtener los snippets que un usuario puede ver
    // Implementa Caso de Uso #5
    fun getAuthorizedSnippets(bearerToken: String): List<Long> {
        val headers = HttpHeaders()
        headers.set("Authorization", bearerToken) // Propagamos el token
        val entity = HttpEntity<Unit>(headers)

        // Llama a GET /authorize/snippets en auth-service
        val response: ResponseEntity<AuthorizedSnippetsDto> =
            restTemplate.exchange(
                "$authServiceUrl/authorize/snippets",
                HttpMethod.GET,
                entity,
                AuthorizedSnippetsDto::class.java,
            )

        return response.body?.snippetIds ?: emptyList()
    }

    // Llama a tu auth-service para verificar un permiso específico
    // Implementa Caso de Uso #6
    fun checkPermission(
        bearerToken: String,
        snippetId: String,
        permission: String,
    ): Boolean {
        val headers = HttpHeaders()
        headers.set("Authorization", bearerToken)
        val entity = HttpEntity<Unit>(headers)

        // Llama a GET /authorize/snippet/{snippetId}/type/{permission}
        val response: ResponseEntity<Boolean> =
            restTemplate.exchange(
                "$authServiceUrl/authorize/snippet/$snippetId/type/$permission",
                HttpMethod.GET,
                entity,
                Boolean::class.java,
            )
        return response.body ?: false
    }

    // Llama a tu auth-service para dar permiso a otro usuario
    // Implementa Caso de Uso #7
    fun grantPermission(
        bearerToken: String,
        snippetId: String,
        userIdToShare: String,
        permission: String,
    ) {
        val headers = HttpHeaders()
        headers.set("Authorization", bearerToken) // Token del *owner*

        val body =
            GrantPermissionDto(
                snippetId = snippetId,
                userId = userIdToShare,
                authorizationType = permission,
            )
        val entity = HttpEntity(body, headers)

        // Llama a POST /authorize/snippet
        restTemplate.exchange(
            "$authServiceUrl/authorize/snippet",
            HttpMethod.POST,
            entity,
            Void::class.java,
        )
    }
}
