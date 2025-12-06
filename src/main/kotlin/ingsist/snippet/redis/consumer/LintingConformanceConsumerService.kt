package ingsist.snippet.redis.consumer

import ingsist.snippet.runner.snippet.dtos.LintingConformanceStatusDTO
import ingsist.snippet.runner.snippet.service.SnippetProcessingService
import org.springframework.stereotype.Service

@Service
class LintingConformanceConsumerService(
    private val snippetProcessingService: SnippetProcessingService,
) : ConsumerConformanceStreamService {
    override fun saveConformance(conformance: LintingConformanceStatusDTO) {
        snippetProcessingService.updateLintingConformance(conformance)
    }
}
