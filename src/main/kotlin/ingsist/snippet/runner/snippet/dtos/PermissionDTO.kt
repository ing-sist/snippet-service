package ingsist.snippet.runner.snippet.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class PermissionDTO(
    val userId: String,
    val snippetId: UUID,
    val permission: String,
)
