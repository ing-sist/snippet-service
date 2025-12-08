package ingsist.snippet.runner.snippet.service

import ingsist.snippet.redis.producer.FormattingSnippetProducer
import ingsist.snippet.redis.producer.LintingSnippetProducer
import ingsist.snippet.runner.snippet.dtos.FormattingRulesDTO
import ingsist.snippet.runner.snippet.dtos.LintingConformanceStatusDTO
import ingsist.snippet.runner.snippet.dtos.LintingRulesDTO
import ingsist.snippet.runner.snippet.dtos.OwnerConfigDto
import ingsist.snippet.runner.snippet.dtos.StreamReqDto
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.runner.user.service.UserService
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RulesService(
    private val snippetRepository: SnippetRepository,
    private val snippetVersionRepository: SnippetVersionRepository,
    private val userService: UserService,
    private val formattingSnippetProducer: FormattingSnippetProducer,
    private val lintingSnippetProducer: LintingSnippetProducer,
) {
    @Transactional
    fun updateLintRules(
        userId: String,
        newRules: LintingRulesDTO,
    ) {
        val currentConfig = userService.getUserConfig(userId)
        val updatedConfig =
            currentConfig.copy(
                noExpressionsInPrintLine = newRules.noExpressionsInPrintLine,
                noUnusedVars = newRules.noUnusedVars,
                noUndefVars = newRules.noUndefVars,
                noUnusedParams = newRules.noUnusedParams,
            )
        userService.updateUserConfig(userId, updatedConfig)
        publishAllSnippetsForLinting(userId, updatedConfig)
    }

    @Transactional
    fun updateFormatRules(
        userId: String,
        newRules: FormattingRulesDTO,
    ) {
        val currentConfig = userService.getUserConfig(userId)
        val updatedConfig =
            currentConfig.copy(
                indentation = newRules.indentation,
                openIfBlockOnSameLine = newRules.openIfBlockOnSameLine,
                maxLineLength = newRules.maxLineLength,
                noTrailingSpaces = newRules.noTrailingSpaces,
                noMultipleEmptyLines = newRules.noMultipleEmptyLines,
            )
        userService.updateUserConfig(userId, updatedConfig)
        publishAllSnippetsForFormatting(userId, updatedConfig)
    }

    fun formatSnippet(
        userId: String,
        snippetId: UUID,
    ) {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }

        if (snippet.ownerId != userId) {
            throw SnippetAccessDeniedException("You don't have permission to format this snippet")
        }

        val config = userService.getUserConfig(userId)
        val assetKey = getSnippetAssetKeyById(snippet.id)
        formattingSnippetProducer.publishSnippet(
            StreamReqDto(
                snippet.id,
                assetKey,
                version = snippet.langVersion,
                language = snippet.language,
                config = config,
            ),
        )
    }

    fun updateLintingConformance(conformance: LintingConformanceStatusDTO) {
        val snippet =
            snippetRepository.findById(conformance.snippetId)
                .orElseThrow {
                    SnippetNotFoundException("Snippet with id ${conformance.snippetId} not found")
                }

        snippet.conformance = conformance.status
        snippetRepository.save(snippet)
    }

    fun getLintingRules(userId: String): LintingRulesDTO {
        val config = userService.getUserConfig(userId)
        return LintingRulesDTO(
            noExpressionsInPrintLine = config.noExpressionsInPrintLine,
            noUnusedVars = config.noUnusedVars,
            noUndefVars = config.noUndefVars,
            noUnusedParams = config.noUnusedParams,
        )
    }

    fun getFormattingRules(userId: String): FormattingRulesDTO {
        val config = userService.getUserConfig(userId)
        return FormattingRulesDTO(
            indentation = config.indentation,
            openIfBlockOnSameLine = config.openIfBlockOnSameLine,
            maxLineLength = config.maxLineLength,
            noTrailingSpaces = config.noTrailingSpaces,
            noMultipleEmptyLines = config.noMultipleEmptyLines,
        )
    }

    private fun getSnippetAssetKeyById(snippetId: UUID): String {
        val snippet =
            snippetVersionRepository.findLatestBySnippetId(snippetId, PageRequest.of(0, 1)).content
                .firstOrNull()
                ?: throw SnippetNotFoundException("Snippet with id $snippetId not found")
        return snippet.assetKey
    }

    private fun publishAllSnippetsForLinting(
        userId: String,
        config: OwnerConfigDto,
    ) {
        val snippets = snippetRepository.findAllByOwnerId(userId, PageRequest.of(0, Int.MAX_VALUE)).content
        snippets.forEach { snippet ->
            val latestVersionPage = snippetVersionRepository.findLatestBySnippetId(snippet.id, PageRequest.of(0, 1))
            if (latestVersionPage.hasContent()) {
                val latestVersion = latestVersionPage.content[0]
                val request =
                    StreamReqDto(
                        id = snippet.id,
                        assetKey = latestVersion.assetKey,
                        version = latestVersion.versionId.toString(),
                        language = snippet.language,
                        config = config,
                    )
                lintingSnippetProducer.publishSnippet(request)
            }
        }
    }

    private fun publishAllSnippetsForFormatting(
        userId: String,
        config: OwnerConfigDto,
    ) {
        val snippets = snippetRepository.findAllByOwnerId(userId, PageRequest.of(0, Int.MAX_VALUE)).content
        snippets.forEach { snippet ->
            val latestVersionPage = snippetVersionRepository.findLatestBySnippetId(snippet.id, PageRequest.of(0, 1))
            if (latestVersionPage.hasContent()) {
                val latestVersion = latestVersionPage.content[0]
                val request =
                    StreamReqDto(
                        id = snippet.id,
                        assetKey = latestVersion.assetKey,
                        version = latestVersion.versionId.toString(),
                        language = snippet.language,
                        config = config,
                    )
                formattingSnippetProducer.publishSnippet(request)
            }
        }
    }
}
