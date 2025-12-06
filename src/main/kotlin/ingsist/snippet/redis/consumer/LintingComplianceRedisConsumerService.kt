package ingsist.snippet.redis.consumer

import ingsist.snippet.runner.snippet.dtos.LintingComplianceStatusDTO
import ingsist.snippet.runner.snippet.service.SnippetProcessingService
import org.springframework.stereotype.Service

@Service
class LintingComplianceRedisConsumerService(
    private val snippetProcessingService: SnippetProcessingService,
) : ConsumerComplianceStreamService {
    override fun saveCompliance(compliance: LintingComplianceStatusDTO) {
        snippetProcessingService.updateLintingCompliance(compliance)
    }
}
