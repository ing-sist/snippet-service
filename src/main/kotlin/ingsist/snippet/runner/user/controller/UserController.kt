package ingsist.snippet.runner.user.controller

import ingsist.snippet.runner.snippet.dtos.OwnerConfigDto
import ingsist.snippet.runner.user.dtos.UserResponseDTO
import ingsist.snippet.runner.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<String> {
        return ResponseEntity.ok("Snippet Service is healthy")
    }

    // US #7: Buscar usuarios para compartir
    @GetMapping
    fun searchUsers(
        @RequestParam email: String,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<List<UserResponseDTO>> {
        val token = principal.token.tokenValue
        val users = userService.searchUsers(email, token)
        return ResponseEntity.ok(users)
    }

    @PostMapping("/updateConfig")
    fun updateUserConfig(
        @RequestParam userId: String,
        @RequestBody config: OwnerConfigDto,
    ) {
        userService.updateUserConfig(userId, config)
    }
}
