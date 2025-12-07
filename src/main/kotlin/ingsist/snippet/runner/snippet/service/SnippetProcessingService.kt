package ingsist.snippet.runner.snippet.service

import StreamReqDto
import ingsist.snippet.redis.producer.FormattingSnippetProducer
import ingsist.snippet.redis.producer.LintingSnippetProducer
import ingsist.snippet.runner.snippet.dtos.LintingConformanceStatusDTO
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.runner.user.service.UserService
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Transactional
class SnippetProcessingService(
    private val snippetRepository: SnippetRepository,
    private val snippetVersionRepository: SnippetVersionRepository,
    private val userService: UserService,
    private val formattingSnippetProducer: FormattingSnippetProducer,
    private val lintingSnippetProducer: LintingSnippetProducer,
) {
    // US #12: Formatting automatico de snippets
    fun formatAllSnippets(userId: String) {
        val config = userService.getUserConfig(userId)
        val allSnippets = snippetRepository.findAllByOwnerId(userId, PageRequest.of(0, Int.MAX_VALUE)).content
        allSnippets.forEach { snippet ->
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

    // US #15: Linting automatico de snippets
    fun lintAllSnippets(userId: String) {
        val config = userService.getUserConfig(userId)
        val allSnippets = snippetRepository.findAllByOwnerId(userId, PageRequest.of(0, Int.MAX_VALUE)).content
        allSnippets.forEach { snippet ->
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
    }

    fun lintSnippet(
        userId: String,
        snippetId: UUID,
    ) {
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

    fun updateLintingConformance(conformance: LintingConformanceStatusDTO) {
        val snippet =
            snippetRepository.findById(conformance.snippetId)
                .orElseThrow {
                    SnippetNotFoundException("Snippet with id ${conformance.snippetId} not found")
                }

        snippet.conformance = conformance.status
        snippetRepository.save(snippet)
    }

    private fun getSnippetAssetKeyById(snippetId: UUID): String {
        val snippet =
            snippetVersionRepository.findLatestBySnippetId(snippetId, PageRequest.of(0, 1)).content
                .firstOrNull()
                ?: throw SnippetNotFoundException("Snippet with id $snippetId not found")
        return snippet.assetKey
    }
}
