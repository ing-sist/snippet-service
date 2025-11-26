package ingsist.snippet.dtos

import java.util.UUID

data class FormatReqDTO(
    val snippetId: UUID,
    val content: String,
    val version: String,
    val config: Map<String, Any>,
)

data class FormatResDTO(
    val snippetId: UUID,
    val content: String,
    val errors: List<String> = emptyList(),
)
