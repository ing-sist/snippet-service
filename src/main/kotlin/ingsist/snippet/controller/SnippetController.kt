package ing.sist.snippet.controller

import ing.sist.snippet.client.dto.ShareSnippetDto
import ing.sist.snippet.model.Snippet
import ing.sist.snippet.service.SnippetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val snippetService: SnippetService,
) {
    /**
     * Endpoint para Caso de Uso #5: Visualizar todos los snippets
     * TODO: Agregar @RequestParam para filtros
     * @RequestParam("name", required = false) name: String?,
     * @RequestParam("language", required = false) language: String?
     */
    @GetMapping
    fun getSnippets(
        @AuthenticationPrincipal principal: Jwt,
    ): ResponseEntity<List<Snippet>> {
        val snippets = snippetService.getSnippetsForUser(principal)
        return ResponseEntity.ok(snippets)
    }

    /**
     * Endpoint para Caso de Uso #6: Visualización de un snippet
     */
    @GetMapping("/{id}")
    fun getSnippetById(
        @AuthenticationPrincipal principal: Jwt,
        @PathVariable id: Long,
    ): ResponseEntity<Snippet> {
        return try {
            val snippet = snippetService.getSnippetById(principal, id)
            if (snippet != null) {
                ResponseEntity.ok(snippet)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    /**
     * Endpoint para Caso de Uso #7: Compartir snippet
     */
    @PostMapping("/{id}/share")
    fun shareSnippet(
        @AuthenticationPrincipal principal: Jwt,
        @PathVariable id: Long,
        @RequestBody shareDto: ShareSnippetDto,
    ): ResponseEntity<Unit> {
        return try {
            snippetService.shareSnippet(principal, id, shareDto)
            ResponseEntity.ok().build()
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(Unit)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(Unit)
        }
    }

    // (Aquí irían los endpoints POST /snippets, PUT /snippets/{id}, DELETE /snippets/{id})
}
