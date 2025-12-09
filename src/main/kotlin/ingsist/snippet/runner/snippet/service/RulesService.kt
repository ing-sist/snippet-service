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
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(RulesService::class.java)

    @Transactional
    fun updateLintRules(
        userId: String,
        newRules: LintingRulesDTO,
    ) {
        log.info("Updating linting rules for user $userId")
        val currentConfig = userService.getUserConfig(userId)
        val updatedConfig =
            currentConfig.copy(
                linting = newRules,
            )
        userService.updateUserConfig(userId, updatedConfig)
        publishAllSnippetsForLinting(userId, updatedConfig)
    }

    @Transactional
    fun updateFormatRules(
        userId: String,
        newRules: FormattingRulesDTO,
    ) {
        log.info("Updating formatting rules for user $userId")
        val currentConfig = userService.getUserConfig(userId)
        val updatedConfig =
            currentConfig.copy(
                formatting = newRules,
            )
        userService.updateUserConfig(userId, updatedConfig)
        publishAllSnippetsForFormatting(userId, updatedConfig)
    }

    fun lintSnippet(
        userId: String,
        snippetId: UUID,
    ) {
        log.info("Linting snippet $snippetId for user $userId")
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFoundException("Snippet with id $snippetId not found") }

        if (snippet.ownerId != userId) {
            throw SnippetAccessDeniedException("You don't have permission to lint this snippet")
        }

        val config = userService.getUserConfig(userId)
        val assetKey = getSnippetAssetKeyById(snippet.id)
        lintingSnippetProducer.publishSnippet(
            StreamReqDto(
                snippet.id,
                assetKey,
                version = snippet.langVersion,
                language = snippet.language,
                config = config,
            ),
        )
    }

    fun formatSnippet(
        userId: String,
        snippetId: UUID,
    ) {
        log.info("Formatting snippet $snippetId for user $userId")
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
        log.info("Updating linting conformance for snippet ${conformance.snippetId} to ${conformance.status}")
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
        return config.linting
    }

    fun getFormattingRules(userId: String): FormattingRulesDTO {
        val config = userService.getUserConfig(userId)
        return config.formatting
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
        log.info("Publishing all snippets for linting for user $userId")
        val snippets = snippetRepository.findAllByOwnerId(userId, PageRequest.of(0, Int.MAX_VALUE)).content
        snippets.forEach { snippet ->
            val latestVersionPage = snippetVersionRepository.findLatestBySnippetId(snippet.id, PageRequest.of(0, 1))
            if (latestVersionPage.hasContent()) {
                val latestVersion = latestVersionPage.content[0]
                val request =
                    StreamReqDto(
                        id = snippet.id,
                        assetKey = latestVersion.assetKey,
                        version = snippet.langVersion,
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
        log.info("Publishing all snippets for formatting for user $userId")
        val snippets = snippetRepository.findAllByOwnerId(userId, PageRequest.of(0, Int.MAX_VALUE)).content
        snippets.forEach { snippet ->
            val latestVersionPage = snippetVersionRepository.findLatestBySnippetId(snippet.id, PageRequest.of(0, 1))
            if (latestVersionPage.hasContent()) {
                val latestVersion = latestVersionPage.content[0]
                val request =
                    StreamReqDto(
                        id = snippet.id,
                        assetKey = latestVersion.assetKey,
                        version = snippet.langVersion,
                        language = snippet.language,
                        config = config,
                    )
                formattingSnippetProducer.publishSnippet(request)
            }
        }
    }
}
