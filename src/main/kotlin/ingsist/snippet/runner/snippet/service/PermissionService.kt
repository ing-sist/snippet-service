package ingsist.snippet.runner.snippet.service

import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.runner.snippet.dtos.PermissionDTO
import ingsist.snippet.shared.exception.ExternalServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PermissionService(
    private val authService: AuthService,
) {
    private val logger = LoggerFactory.getLogger(PermissionService::class.java)

    fun hasReadPermission(
        snippetId: UUID,
        token: String,
    ): Boolean {
        return try {
            authService.hasPermission(snippetId, "READ", token)
        } catch (e: ExternalServiceException) {
            logger.error("Error checking permission for snippet $snippetId: ${e.message}", e)
            false
        }
    }

    fun grantOwnerPermission(
        snippetId: UUID,
        userId: String,
        token: String,
    ) {
        try {
            val permDto = PermissionDTO(userId, snippetId, "OWNER")
            authService.grantPermission(permDto, token)
        } catch (e: ExternalServiceException) {
            logger.error("Error granting owner permission for snippet $snippetId: ${e.message}", e)
        }
    }

    fun grantReadPermission(
        snippetId: UUID,
        userId: String,
        token: String,
    ) {
        val permDto = PermissionDTO(userId, snippetId, "READ")
        authService.grantPermission(permDto, token)
    }

    fun deleteSnippetPermissions(
        snippetId: UUID,
        token: String,
    ) {
        authService.deleteSnippetPermissions(snippetId, token)
    }

    fun getSharedSnippetIds(
        userId: String,
        token: String,
    ): List<UUID> {
        return authService.getSharedSnippets(userId, token).map { it.snippetId }
    }
}
