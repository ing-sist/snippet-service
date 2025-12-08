package ingsist.snippet.runner.snippet.controller

import ingsist.snippet.runner.snippet.dtos.FormattingRulesDTO
import ingsist.snippet.runner.snippet.dtos.LintingRulesDTO
import ingsist.snippet.runner.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.runner.snippet.service.RulesService
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
    @PutMapping("/linting")
    fun updateLintingRules(
        @RequestBody rules: LintingRulesDTO,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        val userId = principal.name
        rulesService.updateLintRules(userId, rules)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/formatting")
    fun updateFormattingRules(
        @RequestBody rules: FormattingRulesDTO,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        val userId = principal.name
        rulesService.updateFormatRules(userId, rules)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/linting")
    fun getLintingRules(principal: JwtAuthenticationToken): ResponseEntity<LintingRulesDTO> {
        val userId = principal.name
        return ResponseEntity.ok(rulesService.getLintingRules(userId))
    }

    @GetMapping("/formatting")
    fun getFormattingRules(principal: JwtAuthenticationToken): ResponseEntity<FormattingRulesDTO> {
        val userId = principal.name
        return ResponseEntity.ok(rulesService.getFormattingRules(userId))
    }

    @PostMapping("/format/{id}")
    fun formatSnippet(
        principal: JwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = principal.token.subject
        rulesService.formatSnippet(userId, id)
        return ResponseEntity.ok().build()
    }
}
