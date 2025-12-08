package ingsist.snippet.runner.snippet.dtos

import java.util.UUID

data class StreamReqDto(
    val id: UUID,
    val assetKey: String,
    val version: String,
    val language: String,
    val config: OwnerConfigDto,
)
