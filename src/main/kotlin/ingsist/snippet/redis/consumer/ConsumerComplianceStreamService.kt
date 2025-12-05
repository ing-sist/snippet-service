package ingsist.snippet.redis.consumer

import ingsist.snippet.runner.snippet.dtos.LintingComplianceStatusDTO

interface ConsumerComplianceStreamService {
    fun saveCompliance(compliance: LintingComplianceStatusDTO)
}
