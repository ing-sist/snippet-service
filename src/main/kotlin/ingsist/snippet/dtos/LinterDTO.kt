package ingsist.snippet.dtos

import java.util.UUID

data class LintReqDTO(
    val snippetId: UUID,
    val content: String,
    val version: String,
    val rules: List<Map<String, Any>>,
)

data class LintResDTO(
    val snippetId: UUID,
    val report: List<String>,
)