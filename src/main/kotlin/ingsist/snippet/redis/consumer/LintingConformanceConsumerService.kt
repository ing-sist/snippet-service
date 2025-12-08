package ingsist.snippet.redis.consumer

import ingsist.snippet.runner.snippet.dtos.LintingConformanceStatusDTO
import ingsist.snippet.runner.snippet.service.RulesService
import org.springframework.stereotype.Service

@Service
class LintingConformanceConsumerService(
    private val rulesService: RulesService,
) : ConsumerConformanceStreamService {
    override fun saveConformance(conformance: LintingConformanceStatusDTO) {
        rulesService.updateLintingConformance(conformance)
    }
}
