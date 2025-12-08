package ingsist.snippet.runner.snippet.controller

import ingsist.snippet.runner.snippet.dtos.FormattingRulesDTO
import ingsist.snippet.runner.snippet.dtos.LintingRulesDTO
import ingsist.snippet.runner.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.runner.snippet.service.RulesService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/rules")
class RulesController(
    private val rulesService: RulesService,
) {
    private val log = LoggerFactory.getLogger(RulesController::class.java)

    @PutMapping("/linting")
    fun updateLintingRules(
        @RequestBody rules: LintingRulesDTO,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        val userId = principal.name
        log.info("Received request to update linting rules for user $userId")
        rulesService.updateLintRules(userId, rules)
        log.info("Linting rules updated successfully for user $userId")
        return ResponseEntity.ok().build()
    }

    @PutMapping("/formatting")
    fun updateFormattingRules(
        @RequestBody rules: FormattingRulesDTO,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        val userId = principal.name
        log.info("Received request to update formatting rules for user $userId")
        rulesService.updateFormatRules(userId, rules)
        log.info("Formatting rules updated successfully for user $userId")
        return ResponseEntity.ok().build()
    }

    @GetMapping("/linting")
    fun getLintingRules(principal: JwtAuthenticationToken): ResponseEntity<LintingRulesDTO> {
        val userId = principal.name
        log.info("Received request to get linting rules for user $userId")
        val rules = rulesService.getLintingRules(userId)
        log.info("Returning linting rules for user $userId")
        return ResponseEntity.ok(rules)
    }

    @GetMapping("/formatting")
    fun getFormattingRules(principal: JwtAuthenticationToken): ResponseEntity<FormattingRulesDTO> {
        val userId = principal.name
        log.info("Received request to get formatting rules for user $userId")
        val rules = rulesService.getFormattingRules(userId)
        log.info("Returning formatting rules for user $userId")
        return ResponseEntity.ok(rules)
    }

    @PostMapping("/format/{id}")
    fun formatSnippet(
        principal: JwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = principal.token.subject
        log.info("Received request to format snippet ID: $id by user $userId")
        rulesService.formatSnippet(userId, id)
        log.info("Snippet ID: $id is being formatted for user $userId")
        return ResponseEntity.ok().build()
    }
}
