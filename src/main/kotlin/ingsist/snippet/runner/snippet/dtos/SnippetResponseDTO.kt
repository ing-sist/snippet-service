package ingsist.snippet.runner.snippet.dtos

import java.util.UUID

data class SnippetResponseDTO(
    val id: UUID,
    val name: String,
    val language: String,
    val description: String,
    val ownerId: String,
    val version: String,
    val conformance: String? = "pending",
    val createdAt: String,
)
