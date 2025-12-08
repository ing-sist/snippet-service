package ingsist.snippet.runner.snippet.dtos

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class ExecuteReqDTO(
    @field:NotBlank val snippetId: UUID,
    @field:NotBlank val content: String,
    @field:NotBlank val version: String,
    @field:NotBlank val language: String,
    @field:NotBlank val assetKey: String,
)

data class ExecuteResDTO(
    val snippetId: UUID,
    val outputs: List<String>,
    val errors: List<String>,
)
