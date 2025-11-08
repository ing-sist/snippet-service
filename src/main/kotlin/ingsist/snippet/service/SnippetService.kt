package ing.sist.snippet.service

import ing.sist.snippet.client.AuthServiceClient
import ing.sist.snippet.client.dto.ShareSnippetDto
import ing.sist.snippet.model.Snippet
import ing.sist.snippet.repository.SnippetRepository
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val authServiceClient: AuthServiceClient,
) {
    // Extrae el ID de usuario de Auth0 ('sub') del token
    private fun getUserIdFrom(principal: Jwt): String = principal.subject

    // Extrae el token completo para propagarlo
    private fun getBearerTokenFrom(principal: Jwt): String = "Bearer ${principal.tokenValue}"

    /**
     * Caso de Uso #5: Visualización de todos los snippets
     */
    fun getSnippetsForUser(principal: Jwt): List<Snippet> {
        val bearerToken = getBearerTokenFrom(principal)

        // 1. Preguntar al Auth Service qué snippets puede ver este usuario
        val authorizedSnippetIds = authServiceClient.getAuthorizedSnippets(bearerToken)

        // 2. Buscar esos snippets en nuestra base de datos local
        return snippetRepository.findByIdIn(authorizedSnippetIds)
        // TODO: Agregar filtrado y paginación
    }

    /**
     * Caso de Uso #6: Visualización de un snippet
     */
    fun getSnippetById(
        principal: Jwt,
        snippetId: Long,
    ): Snippet? {
        val bearerToken = getBearerTokenFrom(principal)

        // 1. Verificar permiso de LECTURA con el auth-service
        val hasPermission =
            authServiceClient.checkPermission(
                bearerToken,
                snippetId.toString(),
                "READ",
            )

        if (!hasPermission) {
            // Puedes lanzar una excepción personalizada aquí
            throw IllegalAccessException("El usuario no tiene permiso para ver este snippet")
        }

        // 2. Si tiene permiso, buscar el snippet
        return snippetRepository.findById(snippetId).orElse(null)
        // TODO: Aquí llamarías al AssetService para obtener el contenido
    }

    /**
     * Caso de Uso #7: Compartir snippet
     */
    fun shareSnippet(
        principal: Jwt,
        snippetId: Long,
        shareDto: ShareSnippetDto,
    ) {
        val ownerId = getUserIdFrom(principal)
        val bearerToken = getBearerTokenFrom(principal)

        // 1. Buscar el snippet para verificar que el usuario es el propietario
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { Exception("Snippet no encontrado") }

        if (snippet.ownerId != ownerId) {
            throw IllegalAccessException("Solo el propietario puede compartir el snippet")
        }

        // 2. Llamar al Auth Service para otorgar el permiso de LECTURA
        try {
            authServiceClient.grantPermission(
                bearerToken,
                snippetId.toString(),
                shareDto.userIdToShareWith,
                "READ",
            )
        } catch (e: HttpClientErrorException) {
            // Manejar errores (ej. usuario a compartir no existe)
            throw Exception("Error al compartir snippet: ${e.message}")
        }
    }

    // (Aquí irían los métodos createSnippet y updateSnippet)
}
