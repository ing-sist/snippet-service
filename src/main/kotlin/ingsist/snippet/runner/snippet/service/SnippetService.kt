package ingsist.snippet.runner.snippet.service

import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.runner.snippet.domain.ValidationResult
import ingsist.snippet.runner.snippet.domain.processEngineResult
import ingsist.snippet.runner.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.runner.snippet.dtos.ExecuteResDTO
import ingsist.snippet.runner.snippet.dtos.SnippetDetailsDTO
import ingsist.snippet.runner.snippet.dtos.SnippetFilterDTO
import ingsist.snippet.runner.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.runner.snippet.dtos.SubmitSnippetDTO
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
import ingsist.snippet.shared.exception.ExternalServiceException
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
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
    private val rulesService: RulesService,
) {
    val log = LoggerFactory.getLogger(SnippetService::class.java)

    fun updateSnippet(
        snippetId: UUID,
        snippet: SubmitSnippetDTO,
        userId: String,
    ): SnippetSubmissionResult {
        log.info("Looking for existing snippet with id: $snippetId in repository")
        val existingSnippet = snippetRepository.findById(snippetId).orElse(null)
        val validationError = validationErrorForUpdate(existingSnippet, userId, snippet)
        if (validationError != null) {
            log.error("Validation error: $validationError for snippet id: $snippetId")
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

        return when (val validationResult = processEngineResult(engineService.parse(request))) {
            is ValidationResult.Invalid -> {
                log.info("Snippet validation with id $snippetId failed: ${validationResult.message}")
                SnippetSubmissionResult.InvalidSnippet(validationResult.message)
            }
            is ValidationResult.Valid -> {
                val updatedSnippet = snippetRepository.save(snippetMetadata.updateWith(snippet))
                snippetVersionRepository.updateAssetKeyIfChanged(lastVersion, assetKey)

                log.info("Snippet with id: $snippetId updated successfully")
                rulesService.lintSnippet(userId, updatedSnippet.id)

                SnippetSubmissionResult.Success(
                    snippetId = updatedSnippet.id,
                    name = updatedSnippet.name,
                    language = updatedSnippet.language,
                )
            }
        }
    }

    fun createSnippet(
        snippet: SubmitSnippetDTO,
        ownerId: String,
        token: String,
    ): SnippetSubmissionResult {
        validateLanguage(snippet, languageService)?.let {
            log.info(
                "Creating snippet failed: Language ${snippet.language} version ${snippet.langVersion} " +
                    "is not supported",
            )
            return invalidSnippet(it)
        }

        val snippetId = UUID.randomUUID()
        val assetKey = assetKeyForLanguage(snippetId, snippet.language, languageService)
        val request = validateRequest(snippet, assetKey, snippetId)

        return when (val validationResult = processEngineResult(engineService.parse(request))) {
            is ValidationResult.Invalid -> {
                log.info("Snippet validation failed: ${validationResult.message}")
                SnippetSubmissionResult.InvalidSnippet(validationResult.message)
            }

            is ValidationResult.Valid -> {
                createSnippetAndPermissions(snippetId, snippet, ownerId, assetKey, token)
            }
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
        log.debug("Snippet version saved successfully for snippetId: {}", snippetId)
        val versionTag = versionTagOrDefault(snippet.versionTag, snippet.langVersion)
        log.debug("Saving snippet metadata for snippetId: {}", snippetId)
        snippetVersionRepository.save(createVersion(snippetMetadata, assetKey, versionTag))
        log.debug("Snippet metadata saved successfully for snippetId: {}", snippetId)
        permissionService.grantOwnerPermission(snippetId, ownerId, token)
        log.debug("Granted owner permission for user: {} on snippetId: {}", ownerId, snippetId)
        log.info("Snippet created successfully with id: {}", snippetId)
        rulesService.lintSnippet(ownerId, snippetId)
        return SnippetSubmissionResult.Success(
            snippetId = snippetMetadata.id,
            name = snippetMetadata.name,
            language = snippetMetadata.language,
        )
    }

    fun getAllSnippets(
        userId: String,
        token: String,
        filter: SnippetFilterDTO,
    ): Page<SnippetResponseDTO> {
        val sharedIds = fetchSharedIds(userId, token, filter.mode)
        if (filter.mode == "SHARED" && sharedIds.isEmpty()) {
            return Page.empty()
        }
        val spec = buildSpecification(userId, sharedIds, filter)
        val pageable = buildPageable(filter)
        val resultPage = snippetRepository.findAll(spec, pageable)

        return resultPage.map { it.toDTO() }
    }

    private fun fetchSharedIds(
        userId: String,
        token: String,
        mode: String,
    ): List<UUID> {
        return if (mode == "SHARED" || mode == "ALL") {
            try {
                permissionService.getSharedSnippetIds(userId, token)
            } catch (e: ExternalServiceException) {
                log.error("Error fetching shared snippets", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun buildSpecification(
        userId: String,
        sharedIds: List<UUID>,
        filter: SnippetFilterDTO,
    ): Specification<SnippetMetadata> {
        var spec: Specification<SnippetMetadata> =
            when (filter.mode) {
                "SHARED" -> SnippetSpecification.isShared(sharedIds)
                "OWNED" -> SnippetSpecification.isOwned(userId)
                else -> {
                    if (sharedIds.isEmpty()) {
                        SnippetSpecification.isOwned(userId)
                    } else {
                        SnippetSpecification.hasAccess(userId, sharedIds)
                    }
                }
            }

        SnippetSpecification.nameContains(filter.name)?.let { spec = spec.and(it) }
        SnippetSpecification.languageEquals(filter.language)?.let { spec = spec.and(it) }
        SnippetSpecification.conformanceEquals(filter.conformance)?.let { spec = spec.and(it) }
        return spec
    }

    private fun buildPageable(filter: SnippetFilterDTO): PageRequest {
        val direction = if (filter.dir.equals("ASC", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        val validSortFields = listOf("name", "language", "conformance", "createdAt")
        val finalSortField = if (validSortFields.contains(filter.sort)) filter.sort else "createdAt"
        return PageRequest.of(filter.page, filter.size, Sort.by(direction, finalSortField))
    }

    fun runSnippet(
        snippetId: UUID,
        userId: String,
        token: String,
        inputs: List<String>,
    ): ExecuteResDTO {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }

        if (snippet.ownerId != userId) {
            if (!permissionService.hasReadPermission(snippetId, token)) {
                throw SnippetAccessDeniedException("You don't have permission to access this snippet")
            }
        }

        val latestVersion =
            snippetVersionRepository.findLatestBySnippetId(snippetId, PageRequest.of(0, 1)).content
                .firstOrNull()
                ?: throw SnippetNotFoundException("Snippet with id $snippetId not found")

        return engineService.execute(
            ExecuteReqDTO(
                snippetId = snippet.id,
                assetKey = latestVersion.assetKey,
                inputs = inputs.toMutableList(),
                version = latestVersion.versionTag,
                language = snippet.language,
            ),
        )
    }

    fun getSnippetById(
        snippetId: UUID,
        userId: String,
        token: String,
    ): SnippetDetailsDTO {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }
        log.debug("Found snippet with id: {}. Checking access permissions for user: {}", snippetId, userId)

        if (snippet.ownerId != userId) {
            if (!permissionService.hasReadPermission(snippetId, token)) {
                throw SnippetAccessDeniedException("You don't have permission to access this snippet")
            }
        }
        log.debug("Access granted for user: {}", userId)

        val assetKey = getSnippetAssetKeyById(snippetId)
        val content = engineService.getSnippetContent(assetKey)

        return SnippetDetailsDTO(
            id = snippet.id,
            name = snippet.name,
            language = snippet.language,
            description = snippet.description,
            ownerId = snippet.ownerId,
            version = snippet.langVersion,
            conformance = snippet.conformance.name,
            createdAt = snippet.createdAt.toString(),
            content = content,
        )
    }

    fun shareSnippet(
        snippetId: UUID,
        targetUserId: String,
        ownerId: String,
        token: String,
    ) {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }

        if (snippet.ownerId != ownerId) {
            throw SnippetAccessDeniedException("You don't have permission to share this snippet (not the owner)")
        }
        log.debug("Granting read permission to user: {} for snippet id: {}", targetUserId, snippetId)

        permissionService.grantReadPermission(snippetId, targetUserId, token)
        log.info("Snippet id: {} shared successfully with user: {}", snippetId, targetUserId)
    }

    fun getSnippetForDownload(
        snippetId: UUID,
        userId: String,
        token: String,
    ): String {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }
        log.debug("Found snippet with id: {}. Checking access permissions for user: {}", snippetId, userId)

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
            throw SnippetAccessDeniedException("You don't have permission to delete this snippet")
        }

        val assetKey = getSnippetAssetKeyById(snippetId)

        try {
            engineService.deleteSnippet(assetKey)
            log.info("Deleted snippet asset with key: {} from engine service", assetKey)
        } catch (e: ExternalServiceException) {
            log.error("Failed to delete asset from Engine: ${e.message}")
            throw e
        }

        snippetRepository.delete(snippet)
        log.info("Deleted snippet metadata with id: {} from repository", snippet.id)

        try {
            permissionService.deleteSnippetPermissions(snippetId, token)
            log.info("Deleted snippet permissions for snippet id: {} from permission service", snippetId)
        } catch (e: ExternalServiceException) {
            log.warn("Warning: Failed to delete permissions for snippet $snippetId. Error: ${e.message}")
        }
    }
}
