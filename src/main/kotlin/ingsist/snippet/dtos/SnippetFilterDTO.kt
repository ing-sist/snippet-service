package ingsist.snippet.dtos

data class SnippetFilterDTO(
    val page: Int = 0,
    val size: Int = 10,
    val name: String? = null,
    val language: String? = null,
    val compliance: String? = null,
    val sort: String = "createdAt",
    val dir: String = "DESC",
)
