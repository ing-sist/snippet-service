package ingsist.snippet.runner.user.controller

import ingsist.snippet.runner.user.dtos.UserResponseDTO
import ingsist.snippet.runner.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {
    private val log = LoggerFactory.getLogger(UserController::class.java)

    // US #7: Buscar usuarios para compartir
    @GetMapping
    fun searchUsers(
        @RequestParam email: String,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<List<UserResponseDTO>> {
        log.info("Received request to search users with email: $email")
        val token = principal.token.tokenValue
        val users = userService.searchUsers(email, token)
        log.info("Found ${users.size} users with email: $email")
        return ResponseEntity.ok(users)
    }
}
