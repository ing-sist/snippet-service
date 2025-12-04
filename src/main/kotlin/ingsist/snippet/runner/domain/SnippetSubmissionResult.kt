package ingsist.snippet.runner.domain

import java.util.UUID

sealed class SnippetSubmissionResult {
    data class Success(
        val snippetId: UUID,
        val name: String,
        val language: String,
    ) : SnippetSubmissionResult()

    data class InvalidSnippet(
        val message: List<String>,
    ) : SnippetSubmissionResult()
}
