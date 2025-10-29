package dtos

data class Snippet(
    val id: String,
    val name: String,
    val language: String,
    val version: String,
    val code: String
)