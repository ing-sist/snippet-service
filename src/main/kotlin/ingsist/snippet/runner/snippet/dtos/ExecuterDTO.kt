package ingsist.snippet.runner.snippet.dtos

import java.util.UUID

data class ExecuteReqDTO(
    val snippetId: UUID,
    val inputs: MutableList<String>,
    val version: String,
    val language: String,
)

data class ExecuteResDTO(
    val snippetId: UUID,
    val outputs: List<String>,
    val errors: List<String>,
)
