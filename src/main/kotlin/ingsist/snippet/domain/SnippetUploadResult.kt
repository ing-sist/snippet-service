package ingsist.snippet.domain

import java.util.UUID

sealed class SnippetUploadResult {
    data class Success(
        val snippetId: UUID,
        val name: String,
        val language: String,
        val version: String,
    ) : SnippetUploadResult()

    data class InvalidSnippet(
        val message: List<String>,
    ) : SnippetUploadResult()
}
