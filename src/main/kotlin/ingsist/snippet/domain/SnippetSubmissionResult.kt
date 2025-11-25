package ingsist.snippet.domain

import java.util.UUID

sealed class SnippetSubmissionResult {
    data class Success(
        val snippetId: UUID,
        val name: String,
        val language: String,
        val version: String,
    ) : SnippetSubmissionResult()

    data class InvalidSnippet(
        val message: List<String>,
    ) : SnippetSubmissionResult()

    companion object
}
