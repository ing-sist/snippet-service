package ingsist.snippet.redis.consumer

import ingsist.snippet.runner.snippet.dtos.LintingConformanceStatusDTO
import ingsist.snippet.runner.snippet.service.SnippetService
import org.springframework.stereotype.Service

@Service
class LintingConformanceConsumerService(
    private val snippetService: SnippetService,
) : ConsumerConformanceStreamService {
    override fun saveConformance(conformance: LintingConformanceStatusDTO) {
        snippetService.updateLintingConformance(conformance)
    }
}
