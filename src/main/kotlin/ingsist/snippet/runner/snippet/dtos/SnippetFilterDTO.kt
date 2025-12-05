package ingsist.snippet.runner.snippet.dtos

data class SnippetFilterDTO(
    val page: Int = 0,
    val size: Int = 10,
    val name: String? = null,
    val language: String? = null,
    val compliance: String? = null,
    val mode: String = "ALL",
    val sort: String = "createdAt",
    val dir: String = "DESC",
)
