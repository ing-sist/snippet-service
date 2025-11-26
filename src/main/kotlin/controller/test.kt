package controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class test(
    @Value("\${app.environment}") private val environment: String
) {
    @GetMapping("/env")
    fun getEnvironment(): String {
        return "Running in environment: $environment"
    }
}