package ingsist.snippet.runner.health

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/health")
class HealthController {
    val log = LoggerFactory.getLogger(HealthController::class.java)

    @GetMapping("/check")
    fun healthCheck(): ResponseEntity<String> {
        log.info("Health check requested")
        log.info("Snippet Service is healthy")
        return ResponseEntity.ok("Snippet Service is healthy")
    }

    @GetMapping("/error")
    fun errorCheck(): ResponseEntity<String> {
        log.info("Simulated error endpoint called")
        log.error("Snippet Service has a simulated error")
        return ResponseEntity.status(500).body("Snippet Service has an error")
    }
}
