package dtos


data class SnippetMetaData(
    val id: String,
    val name: String,
    val language: String,
    val version: String,
    val description: String,
    val assetKey: String,
)