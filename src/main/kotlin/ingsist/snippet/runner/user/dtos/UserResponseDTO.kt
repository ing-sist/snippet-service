package ingsist.snippet.runner.user.dtos

import com.fasterxml.jackson.annotation.JsonAlias

data class UserResponseDTO(
    @JsonAlias("userId")
    val id: String,
    @JsonAlias("userEmail")
    val email: String,
)
