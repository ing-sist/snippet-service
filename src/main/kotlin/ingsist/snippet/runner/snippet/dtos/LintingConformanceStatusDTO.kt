package ingsist.snippet.runner.snippet.dtos

import ingsist.snippet.runner.snippet.domain.ConformanceStatus
import java.util.UUID

data class LintingConformanceStatusDTO(
    val snippetId: UUID,
    val status: ConformanceStatus,
    val correlationId: String? = null,
)
