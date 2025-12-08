package ingsist.snippet.runner.snippet.service

import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.runner.snippet.domain.ValidationResult
import ingsist.snippet.runner.snippet.domain.processEngineResult
import ingsist.snippet.runner.snippet.dtos.SnippetFilterDTO
import ingsist.snippet.runner.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.runner.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.runner.snippet.dtos.ValidateReqDto
import ingsist.snippet.runner.snippet.helpers.adjustedAssetKey
import ingsist.snippet.runner.snippet.helpers.assetKeyForLanguage
import ingsist.snippet.runner.snippet.helpers.createMetadata
import ingsist.snippet.runner.snippet.helpers.createVersion
import ingsist.snippet.runner.snippet.helpers.invalidSnippet
import ingsist.snippet.runner.snippet.helpers.toDTO
import ingsist.snippet.runner.snippet.helpers.updateAssetKeyIfChanged
import ingsist.snippet.runner.snippet.helpers.updateWith
import ingsist.snippet.runner.snippet.helpers.validateLanguage
import ingsist.snippet.runner.snippet.helpers.validateRequest
import ingsist.snippet.runner.snippet.helpers.versionTagOrDefault
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetSpecification
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val snippetVersionRepository: SnippetVersionRepository,
    private val engineService: EngineService,
    private val permissionService: PermissionService,
    private val languageService: LanguageService,
) {
    // US #2 & #4: Actualizar snippet (Owner Aware)
    fun updateSnippet(
        snippetId: UUID,
        snippet: SubmitSnippetDTO,
        userId: String,
    ): SnippetSubmissionResult {
        val existingSnippet = snippetRepository.findById(snippetId).orElse(null)
        val validationError = validationErrorForUpdate(existingSnippet, userId, snippet)
        if (validationError != null) {
            return invalidSnippet(validationError)
        }

        val snippetMetadata = existingSnippet!!
        val lastVersion = snippetMetadata.versions.last()
        val assetKey =
            adjustedAssetKey(
                snippetId = snippetId,
                newLanguage = snippet.language,
                currentLanguage = snippetMetadata.language,
                currentAssetKey = lastVersion.assetKey,
                languageService = languageService,
            )
        val request = validateRequest(snippet, assetKey, snippetMetadata.id)

        return when (val validationResult = validateSnippet(request)) {
            is ValidationResult.Invalid -> SnippetSubmissionResult.InvalidSnippet(validationResult.message)
            is ValidationResult.Valid -> {
                val updatedSnippet = snippetRepository.save(snippetMetadata.updateWith(snippet))
                snippetVersionRepository.updateAssetKeyIfChanged(lastVersion, assetKey)

                SnippetSubmissionResult.Success(
                    snippetId = updatedSnippet.id,
                    name = updatedSnippet.name,
                    language = updatedSnippet.language,
                )
            }
        }
    }

    // US #1 & #3: Crear snippet (Con OwnerId)
    fun createSnippet(
        snippet: SubmitSnippetDTO,
        ownerId: String,
        token: String,
    ): SnippetSubmissionResult {
        validateLanguage(snippet, languageService)?.let { return invalidSnippet(it) }

        val snippetId = UUID.randomUUID()
        val assetKey = assetKeyForLanguage(snippetId, snippet.language, languageService)
        val request = validateRequest(snippet, assetKey, snippetId)

        return when (val validationResult = validateSnippet(request)) {
            is ValidationResult.Invalid -> SnippetSubmissionResult.InvalidSnippet(validationResult.message)
            is ValidationResult.Valid -> createSnippetAndPermissions(snippetId, snippet, ownerId, assetKey, token)
        }
    }

    private fun validationErrorForUpdate(
        snippetMetadata: SnippetMetadata?,
        userId: String,
        snippet: SubmitSnippetDTO,
    ): String? {
        val languageError = validateLanguage(snippet, languageService)
        return when {
            snippetMetadata == null -> "Snippet not found"
            snippetMetadata.ownerId != userId -> "You are not the owner of this snippet"
            languageError != null -> languageError
            else -> null
        }
    }

    private fun createSnippetAndPermissions(
        snippetId: UUID,
        snippet: SubmitSnippetDTO,
        ownerId: String,
        assetKey: String,
        token: String,
    ): SnippetSubmissionResult.Success {
        val snippetMetadata = snippetRepository.save(createMetadata(snippetId, snippet, ownerId))
        val versionTag = versionTagOrDefault(snippet.versionTag, snippet.langVersion)
        snippetVersionRepository.save(createVersion(snippetMetadata, assetKey, versionTag))
        permissionService.grantOwnerPermission(snippetId, ownerId, token)

        return SnippetSubmissionResult.Success(
            snippetId = snippetMetadata.id,
            name = snippetMetadata.name,
            language = snippetMetadata.language,
        )
    }

    private fun validateSnippet(snippet: ValidateReqDto): ValidationResult {
        return processEngineResult(engineService.parse(snippet))
    }

    // US #5: Listar snippets
    fun getAllSnippets(
        userId: String,
        token: String,
        filter: SnippetFilterDTO,
    ): List<SnippetResponseDTO> {
        // 1. Obtener IDs compartidos desde Auth Service (solo si hace falta)
        val sharedIds: List<UUID> =
            if (filter.mode == "SHARED" || filter.mode == "ALL") {
                permissionService.getSharedSnippetIds(userId, token)
            } else {
                emptyList()
            }

        // Caso borde: Si pide SHARED y no tiene nada compartido, retornar vacío
        if (filter.mode == "SHARED" && sharedIds.isEmpty()) {
            return emptyList()
        }

        // 2. Construir Specification Base según el MODO
        var spec: Specification<SnippetMetadata> =
            when (filter.mode) {
                "SHARED" -> SnippetSpecification.isShared(sharedIds) // Solo IDs compartidos
                "OWNED" -> SnippetSpecification.isOwned(userId) // Solo OwnerId = userId
                else -> SnippetSpecification.hasAccess(userId, sharedIds) // (Owner OR Shared) -> ALL
            }

        SnippetSpecification.nameContains(filter.name)?.let { spec = spec.and(it) }
        SnippetSpecification.languageEquals(filter.language)?.let { spec = spec.and(it) }
        SnippetSpecification.conformanceEquals(filter.conformance)?.let { spec = spec.and(it) }

        // 3. Construir Pageable con Ordenamiento (Sort)
        val direction = if (filter.dir.equals("ASC", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC

        // Validamos que el campo sea seguro para ordenar, sino default createdAt
        val validSortFields = listOf("name", "language", "conformance", "createdAt")
        val finalSortField = if (validSortFields.contains(filter.sort)) filter.sort else "createdAt"

        val pageable = PageRequest.of(filter.page, filter.size, Sort.by(direction, finalSortField))

        // 4. Ejecutar consulta
        val resultPage = snippetRepository.findAll(spec, pageable)

        return resultPage.content.map { it.toDTO() }
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

        permissionService.grantReadPermission(snippetId, targetUserId, token)
    }

    fun getSnippetForDownload(
        snippetId: UUID,
        userId: String,
        token: String,
    ): String {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }

        if (snippet.ownerId != userId) {
            if (!permissionService.hasReadPermission(snippetId, token)) {
                throw SnippetAccessDeniedException("You don't have permission to access this snippet")
            }
        }

        return getSnippetAssetKeyById(snippetId)
    }

    fun getSnippetAssetKeyById(snippetId: UUID): String {
        val snippet =
            snippetVersionRepository.findLatestBySnippetId(snippetId, PageRequest.of(0, 1)).content
                .firstOrNull()
                ?: throw SnippetNotFoundException("Snippet with id $snippetId not found")
        return snippet.assetKey
    }

    fun deleteSnippet(
        snippetId: UUID,
        userId: String,
        token: String,
    ) {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }

        if (snippet.ownerId != userId) {
            throw SnippetAccessDeniedException("You don't have permission to delete this snippet (not the owner)")
        }

        val assetKey = getSnippetAssetKeyById(snippetId)
        engineService.deleteSnippet(assetKey)
        snippetRepository.delete(snippet)
        permissionService.deleteSnippetPermissions(snippetId, token)
    }
}
