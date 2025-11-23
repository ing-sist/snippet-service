package ingsist.snippet.dtos

import jakarta.validation.constraints.NotBlank

data class SnippetParserRequestDTO(
    @field:NotBlank val code: String,
    @field:NotBlank val language: String,
    @field:NotBlank val version: String,
)