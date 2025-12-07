package ingsist.snippet.runner.snippet.dtos

data class SupportedLanguageDto(
    val name: String,
    val version: List<String>,
    val extension: String,
)
