package ingsist.snippet.service

import ingsist.snippet.asset.AssetService
import ingsist.snippet.client.AuthClient
import ingsist.snippet.domain.SnippetMetadata
import ingsist.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.domain.SnippetVersion
import ingsist.snippet.domain.ValidationResult
import ingsist.snippet.domain.processEngineResult
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.engine.EngineService
import ingsist.snippet.exception.SnippetAccessDeniedException
import ingsist.snippet.exception.SnippetNotFoundException
import ingsist.snippet.redis.formatter.FormattingSnippetProducer
import ingsist.snippet.repository.SnippetRepository
import ingsist.snippet.repository.SnippetVersionRepository
import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val snippetVersionRepository: SnippetVersionRepository,
    private val assetService: AssetService,
    private val engineService: EngineService,
    private val authClient: AuthClient,
    private val snippetProducer: FormattingSnippetProducer,
) {
    // US #2 & #4: Actualizar snippet (Owner Aware)
    fun updateSnippet(
        snippetId: UUID,
        newCode: String,
        userId: String,
    ): SnippetSubmissionResult {
        val existingSnippet = snippetRepository.findById(snippetId).orElse(null)

        val validationError =
            when {
                existingSnippet == null -> "Snippet not found"
                existingSnippet.ownerId != userId -> "You are not the owner of this snippet"
                else -> null
            }

        if (validationError != null) {
            return SnippetSubmissionResult.InvalidSnippet(listOf(validationError))
        }

        val lastVersion = existingSnippet.versions.last()

        val request =
            ExecuteReqDTO(
                code = newCode,
                language = existingSnippet.language,
                version = lastVersion.versionTag,
                snippetId = existingSnippet.id,
            )

        return when (val validationResult = validateSnippet(request)) {
            is ValidationResult.Valid -> {
                assetService.update("snippets", lastVersion.assetKey, request.code)
                SnippetSubmissionResult.Success(
                    snippetId = existingSnippet.id,
                    name = existingSnippet.name,
                    language = existingSnippet.language,
                    version = lastVersion.versionTag,
                )
            }
            is ValidationResult.Invalid -> {
                SnippetSubmissionResult.InvalidSnippet(validationResult.message)
            }
        }
    }

    // US #1 & #3: Crear snippet (Con OwnerId)
    fun createSnippet(
        snippet: SubmitSnippetDTO,
        ownerId: String,
    ): SnippetSubmissionResult {
        val snippetId = UUID.randomUUID()
        val assetKey = "snippet-$snippetId.ps"

        val request =
            ExecuteReqDTO(
                code = snippet.code,
                language = snippet.language,
                version = snippet.version,
                snippetId = snippetId,
            )
        return when (val validationResult = validateSnippet(request)) {
            is ValidationResult.Invalid -> {
                SnippetSubmissionResult.InvalidSnippet(validationResult.message)
            }
            is ValidationResult.Valid -> {
                assetService.upload("snippets", assetKey, snippet.code)

                // Guardar Metadata con OwnerId
                val snippetMetadata =
                    SnippetMetadata(
                        id = snippetId,
                        name = snippet.name,
                        language = snippet.language,
                        description = snippet.description,
                        ownerId = ownerId,
                    )
                snippetRepository.save(snippetMetadata)

                val snippetVersion =
                    SnippetVersion(
                        versionId = UUID.randomUUID(),
                        snippet = snippetMetadata,
                        assetKey = assetKey,
                        createdDate = Date(),
                        versionTag = snippet.versionTag ?: "1.0",
                    )
                snippetVersionRepository.save(snippetVersion)

                SnippetSubmissionResult.Success(
                    snippetId = snippetMetadata.id,
                    name = snippetMetadata.name,
                    language = snippetMetadata.language,
                    version = snippetVersion.versionTag,
                )
            }
        }
    }

    private fun validateSnippet(snippet: ExecuteReqDTO): ValidationResult {
        return processEngineResult(engineService.parse(snippet))
    }

    // Helper para DTO
    private fun SnippetMetadata.toDTO(): SnippetResponseDTO {
        val lastVersion = this.versions.maxByOrNull { it.createdDate }
        return SnippetResponseDTO(
            id = this.id,
            name = this.name,
            language = this.language,
            description = this.description,
            ownerId = this.ownerId,
            version = lastVersion?.versionTag ?: "1.0",
            compliance = "pending",
        )
    }

    // US #5: Listar snippets
    fun getAllSnippets(
        userId: String,
        page: Int,
        size: Int,
        token: String,
    ): List<SnippetResponseDTO> {
        // 1. Obtener IDs compartidos desde Auth Service
        val sharedIds = authClient.getUserPermissions(userId, token)

        val pageable = PageRequest.of(page, size)

        // 2. Buscar (Mis snippets + Compartidos)
        val snippetsPage =
            if (sharedIds.isEmpty()) {
                snippetRepository.findAllByOwnerId(userId, pageable)
            } else {
                snippetRepository.findAllByOwnerIdOrIdIn(userId, sharedIds, pageable)
            }

        return snippetsPage.content.map { it.toDTO() }
    }

    // US #6: Obtener snippet por ID
    fun getSnippetById(snippetId: UUID): SnippetResponseDTO {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }
        return snippet.toDTO()
    }

    // US #7: Compartir snippet
    fun shareSnippet(
        snippetId: UUID,
        targetUserId: String,
        ownerId: String,
        token: String,
    ) {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }

        // Validación de Owner
        if (snippet.ownerId != ownerId) {
            throw SnippetAccessDeniedException("You don't have permission to share this snippet (not the owner)")
        }

        authClient.shareSnippet(snippetId, targetUserId, token)
    }

    // US #12: Formatting automatico de snippets
    fun formatAllSnippets(userId: String) {
        val allSnippets = snippetRepository.findAllByOwnerId(userId,  PageRequest.of(0, Int.MAX_VALUE)).content
        allSnippets.forEach { snippet ->
            snippetProducer.publishSnippet(snippet.id)
        }
    }
}
