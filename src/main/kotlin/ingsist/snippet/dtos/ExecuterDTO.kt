package ingsist.snippet.dtos

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class ExecuteReqDTO(
    @field:NotBlank val snippetId: UUID,
    @field:NotBlank val content: String,
    @field:NotBlank val version: String,
    val inputs: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
)

data class ExecuteResDTO(
    val snippetId: UUID,
    val outputs: List<String>,
    val errors: List<String>,
)