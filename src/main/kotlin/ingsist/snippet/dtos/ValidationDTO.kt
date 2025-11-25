package ingsist.snippet.dtos

import java.util.UUID

data class ValidateReqDto(
    val snippetId: UUID,
    val content: String,
    val version: String,
)

data class ValidateResDto(
    val snippetId: UUID,
    val error: List<String>,
)
