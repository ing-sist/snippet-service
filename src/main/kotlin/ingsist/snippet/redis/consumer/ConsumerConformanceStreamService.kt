package ingsist.snippet.redis.consumer

import ingsist.snippet.runner.snippet.dtos.LintingConformanceStatusDTO

interface ConsumerConformanceStreamService {
    fun saveConformance(conformance: LintingConformanceStatusDTO)
}
