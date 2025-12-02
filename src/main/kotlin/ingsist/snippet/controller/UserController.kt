package ingsist.snippet.controller

import ingsist.snippet.domain.OwnerConfig
import ingsist.snippet.dtos.OwnerConfigDTO
import ingsist.snippet.service.UserService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import java.util.UUID

@Service
@Transactional
class UserController(
    private val userService: UserService,
) {
    @PostMapping("/updateConfig")
    fun updateUserConfig(
        userId: String,
        config: OwnerConfigDTO,
    ) {
        userService.updateUserConfig(userId, config)
    }

    @GetMapping("/userConfig")
    fun getUserConfig(snippetId: UUID): OwnerConfig {
        return userService.getUserConfig(snippetId)
    }
}
