package ingsist.snippet.dtos

import jakarta.validation.constraints.NotBlank

data class SnippetUploadDTO(
    @field:NotBlank val name: String,
    @field:NotBlank val language: String,
    @field:NotBlank val version: String,
    @field:NotBlank val description: String,
)