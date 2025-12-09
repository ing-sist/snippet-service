package ingsist.snippet.shared.exception

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(GlobalExceptionHandlerTest.TestController::class)
@org.springframework.test.context.ActiveProfiles("test")
@Import(GlobalExceptionHandler::class)
class GlobalExceptionHandlerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @TestConfiguration
    class TestConfig {
        @Bean
        fun testController() = TestController()
    }

    @RestController
    class TestController {
        @GetMapping("/test/not-found")
        fun notFound() {
            throw SnippetNotFoundException("Not found")
        }

        @GetMapping("/test/access-denied")
        fun accessDenied() {
            throw SnippetAccessDeniedException("Access denied")
        }

        @GetMapping("/test/invalid")
        fun invalid() {
            throw InvalidSnippetException(listOf("Invalid"))
        }

        @GetMapping("/test/external")
        fun external() {
            throw ExternalServiceException("External error")
        }

        @GetMapping("/test/generic")
        @Suppress("TooGenericExceptionThrown")
        fun generic() {
            throw RuntimeException("Generic error")
        }
    }

    @Test
    fun `should handle SnippetNotFoundException`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/not-found").with(jwt()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should handle SnippetAccessDeniedException`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/access-denied").with(jwt()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should handle InvalidSnippetException`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/invalid").with(jwt()))
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `should handle ExternalServiceException`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/external").with(jwt()))
            .andExpect(status().isBadGateway)
    }

    @Test
    fun `should handle generic Exception`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/generic").with(jwt()))
            .andExpect(status().isInternalServerError)
    }
}
