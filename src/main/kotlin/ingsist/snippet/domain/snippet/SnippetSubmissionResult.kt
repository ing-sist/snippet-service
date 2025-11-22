package ingsist.snippet.domain.snippet

sealed class SnippetSubmissionResult {
    data class Success(
        val snippetId: String,
        val name: String,
        val language: String,
        val version: String,
    ) : SnippetSubmissionResult()

    data class ValidatedSnippet(
        val name: String,
        val language: String,
        val version: String,
    ) : SnippetSubmissionResult()

    data class InvalidSnippet(
        val ruleViolated: String,
        val line: Int,
        val column: Int,
        val detail: String
    ) : SnippetSubmissionResult()

    data class UnsupportedLanguage(
        val language: String,
        val version: String
    ) : SnippetSubmissionResult()
}