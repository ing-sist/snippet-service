package ingsist.snippet.domain.snippet

sealed class SnippetUploadResult {
    data class Success(
        val snippetId: String,
        val name: String,
        val language: String,
        val version: String,
    ) : SnippetUploadResult()

    data class InvalidSnippet(
        val ruleViolated: String,
        val line: Int,
        val column: Int,
        val detail: String
    ) : SnippetUploadResult()

    data class UnsupportedLanguage(
        val language: String,
        val version: String
    ) : SnippetUploadResult()
}