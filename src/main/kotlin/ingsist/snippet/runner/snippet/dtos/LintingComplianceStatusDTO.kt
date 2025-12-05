package ingsist.snippet.runner.snippet.dtos

import ingsist.snippet.runner.snippet.domain.ComplianceStatus
import java.util.UUID

data class LintingComplianceStatusDTO(
    val snippetId: UUID,
    val status: ComplianceStatus,
)
