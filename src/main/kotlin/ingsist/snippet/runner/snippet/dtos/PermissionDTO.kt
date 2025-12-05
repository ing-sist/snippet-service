package ingsist.snippet.runner.snippet.dtos

import java.util.UUID

data class PermissionDTO(
    val userId: String,
    val snippetId: UUID,
    val permission: String,
)
