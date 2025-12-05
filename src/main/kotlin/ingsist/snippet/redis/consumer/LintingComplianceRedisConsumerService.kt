package ingsist.snippet.redis.consumer

import ingsist.snippet.runner.snippet.dtos.LintingComplianceStatusDTO
import ingsist.snippet.runner.snippet.service.SnippetService
import org.springframework.stereotype.Service

@Service
class LintingComplianceRedisConsumerService(
    private val snippetService: SnippetService,
) : ConsumerComplianceStreamService {
    override fun saveCompliance(compliance: LintingComplianceStatusDTO) {
        snippetService.updateLintingCompliance(compliance)
    }
}
