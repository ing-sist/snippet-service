package ingsist.snippet.runner.snippet.controller

import ingsist.snippet.runner.snippet.domain.LanguageConfig
import ingsist.snippet.runner.snippet.service.LanguageService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/languages")
class LanguageController(
    private val languageService: LanguageService,
) {
    val log = LoggerFactory.getLogger(LanguageController::class.java)

    @GetMapping
    fun getSupportedLanguages(): ResponseEntity<List<LanguageConfig>> {
        val languages = languageService.getSupportedLanguages()
        log.info("Returning {} supported languages", languages.size)
        return ResponseEntity.ok(languages)
    }
}
