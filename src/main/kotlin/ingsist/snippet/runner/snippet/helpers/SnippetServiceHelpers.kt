package ingsist.snippet.runner.snippet.helpers

import ingsist.snippet.runner.snippet.domain.ConformanceStatus
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.runner.snippet.domain.SnippetVersion
import ingsist.snippet.runner.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.runner.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.runner.snippet.dtos.ValidateReqDto
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.runner.snippet.service.LanguageService
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

internal fun validateLanguage(
    snippet: SubmitSnippetDTO,
    languageService: LanguageService,
): String? {
    return if (languageService.isLanguageSupported(snippet.language, snippet.langVersion)) {
        null
    } else {
        "Language ${snippet.language} version ${snippet.langVersion} is not supported"
    }
}

internal fun invalidSnippet(message: String): SnippetSubmissionResult {
    return SnippetSubmissionResult.InvalidSnippet(listOf(message))
}

internal fun validateRequest(
    snippet: SubmitSnippetDTO,
    assetKey: String,
    snippetId: UUID,
): ValidateReqDto {
    return ValidateReqDto(
        content = snippet.code,
        snippetId = snippetId,
        assetKey = assetKey,
        version = snippet.langVersion,
        language = snippet.language,
    )
}

internal fun SnippetMetadata.updateWith(snippet: SubmitSnippetDTO): SnippetMetadata {
    return copy(
        name = snippet.name,
        language = snippet.language,
        langVersion = snippet.langVersion,
        description = snippet.description,
    )
}

internal fun createMetadata(
    snippetId: UUID,
    snippet: SubmitSnippetDTO,
    ownerId: String,
): SnippetMetadata {
    return SnippetMetadata(
        id = snippetId,
        name = snippet.name,
        language = snippet.language,
        description = snippet.description,
        ownerId = ownerId,
        langVersion = snippet.langVersion,
        conformance = ConformanceStatus.PENDING,
        createdAt = LocalDateTime.now(),
    )
}

internal fun createVersion(
    snippetMetadata: SnippetMetadata,
    assetKey: String,
    versionTag: String,
): SnippetVersion {
    return SnippetVersion(
        versionId = UUID.randomUUID(),
        assetKey = assetKey,
        createdDate = Date(),
        versionTag = versionTag,
        snippet = snippetMetadata,
    )
}

internal fun SnippetVersionRepository.updateAssetKeyIfChanged(
    snippetVersion: SnippetVersion,
    assetKey: String,
) {
    if (assetKey != snippetVersion.assetKey) {
        snippetVersion.assetKey = assetKey
        save(snippetVersion)
    }
}

internal fun SnippetMetadata.toDTO(): SnippetResponseDTO {
    return SnippetResponseDTO(
        id = this.id,
        name = this.name,
        language = this.language,
        description = this.description,
        ownerId = this.ownerId,
        conformance = this.conformance.name,
        version = langVersion,
        createdAt = this.createdAt.toString(),
    )
}

internal fun versionTagOrDefault(
    versionTag: String?,
    defaultTag: String,
): String {
    return versionTag?.takeIf { it.isNotBlank() } ?: defaultTag
}
