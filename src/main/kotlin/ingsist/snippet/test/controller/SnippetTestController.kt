package ingsist.snippet.test.controller

import ingsist.snippet.test.model.dto.CreateTestRequest
import ingsist.snippet.test.model.dto.RunTestResponse
import ingsist.snippet.test.model.dto.SnippetTestResponse
import ingsist.snippet.test.service.SnippetTestService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/snippets/{snippetId}/tests")
class SnippetTestController(
    private val snippetTestService: SnippetTestService,
) {
    @PostMapping
    fun createTest(
        @PathVariable snippetId: UUID,
        @RequestBody request: CreateTestRequest,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<SnippetTestResponse> {
        val ownerId = principal.token.subject
        val created = snippetTestService.createTest(snippetId, ownerId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping
    fun listTests(
        @PathVariable snippetId: UUID,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<List<SnippetTestResponse>> {
        val requesterId = principal.token.subject
        val token = principal.token.tokenValue
        val tests = snippetTestService.listTests(snippetId, requesterId, token)
        return ResponseEntity.ok(tests)
    }

    @DeleteMapping("/{testId}")
    fun deleteTest(
        @PathVariable snippetId: UUID,
        @PathVariable testId: UUID,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        val ownerId = principal.token.subject
        snippetTestService.deleteTest(snippetId, testId, ownerId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{testId}/run")
    fun runTest(
        @PathVariable snippetId: UUID,
        @PathVariable testId: UUID,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<RunTestResponse> {
        val requesterId = principal.token.subject
        val token = principal.token.tokenValue
        val result = snippetTestService.runTest(snippetId, testId, requesterId, token)
        return ResponseEntity.ok(result)
    }
}
