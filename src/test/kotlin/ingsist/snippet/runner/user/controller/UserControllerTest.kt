package ingsist.snippet.runner.user.controller

import ingsist.snippet.runner.user.dtos.UserResponseDTO
import ingsist.snippet.runner.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController::class)
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtDecoder: JwtDecoder

    @MockBean
    private lateinit var userService: UserService

    @Test
    fun `should search users`() {
        val email = "test@example.com"
        val response = listOf(UserResponseDTO("user-1", email))

        Mockito.`when`(userService.searchUsers(Mockito.anyString() ?: "", Mockito.anyString() ?: ""))
            .thenReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/users")
                .param("email", email)
                .with(jwt().jwt { it.subject("user-1") }),
        )
            .andExpect(status().isOk)
    }
}
