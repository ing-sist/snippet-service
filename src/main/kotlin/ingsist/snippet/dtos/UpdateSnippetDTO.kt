package ingsist.snippet.dtos

import jakarta.validation.constraints.NotBlank

data class UpdateSnippetDTO(
    @field:NotBlank var name: String,
    var newName: String?,
    var newCode: String?,
    var newLanguage: String?,
    var newVersion: String?,
    var newDescription: String?,
)