package ingsist.snippet.runner.controller

import ingsist.snippet.runner.domain.OwnerConfig
import ingsist.snippet.runner.dtos.OwnerConfigDTO
import ingsist.snippet.runner.service.UserService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Service
@Transactional
@RequestMapping("/user")
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

    @GetMapping("/{snippetId}/getConfig")
    fun getUserConfigBySnippetID(
        @PathVariable snippetId: UUID,
    ): OwnerConfig {
        return userService.getUserConfig(snippetId)
    }
}
