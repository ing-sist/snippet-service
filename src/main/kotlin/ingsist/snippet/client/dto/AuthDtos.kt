package ing.sist.snippet.client.dto

// DTOs para la comunicación con auth-service

// DTO para Pedir permisos
data class GrantPermissionDto(
    val snippetId: String,
    val userId: String,
    val authorizationType: String,
)

// DTO para Obtener snippets autorizados (Respuesta de tu auth-service)
data class AuthorizedSnippetsDto(
    val snippetIds: List<Long>,
)

// DTO para la Petición de compartir (Cuerpo de nuestra API)
data class ShareSnippetDto(
    val userIdToShareWith: String,
)
