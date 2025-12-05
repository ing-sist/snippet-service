package ingsist.snippet.runner.snippet.dtos

import jakarta.validation.constraints.NotBlank

data class SubmitSnippetDTO(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
    @field:NotBlank val language: String,
    @field:NotBlank val langVersion: String,
    @field:NotBlank val description: String,
    val versionTag: String? = null,
)
