package ingsist.snippet.runner.snippet.controller

import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.runner.snippet.dtos.SnippetFilterDTO
import ingsist.snippet.runner.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.runner.snippet.dtos.SnippetUploadDTO
import ingsist.snippet.runner.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.runner.snippet.service.SnippetProcessingService
import ingsist.snippet.runner.snippet.service.SnippetService
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val snippetService: SnippetService,
    private val engineService: EngineService,
    private val snippetProcessingService: SnippetProcessingService,
) {
    // US #1: Crear snippet (Upload file)
    @PostMapping("/upload-from-file")
    fun uploadSnippetFromFile(
        @RequestParam("file") file: MultipartFile,
        @Valid params: SnippetUploadDTO,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Any> {
        val code = file.bytes.toString(Charsets.UTF_8)
        val userId = principal.token.subject
        val token = principal.token.tokenValue
        return uploadSnippetLogic(code, params, userId, token)
    }

    // US #3: Crear snippet (Editor)
    @PostMapping("/upload-inline")
    fun uploadSnippetInline(
        @RequestBody code: String,
        @Valid params: SnippetUploadDTO,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Any> {
        val userId = principal.token.subject
        val token = principal.token.tokenValue
        return uploadSnippetLogic(code, params, userId, token)
    }

    // Lógica común de creación
    private fun uploadSnippetLogic(
        code: String,
        params: SnippetUploadDTO,
        ownerId: String,
        token: String,
    ): ResponseEntity<Any> {
        val snippet =
            SubmitSnippetDTO(
                code,
                params.name,
                params.language,
                params.version,
                params.description,
                params.versionTag ?: "",
            )
        // Pasamos el ownerId al servicio
        val result = snippetService.createSnippet(snippet, ownerId, token)

        return when (result) {
            is SnippetSubmissionResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(result)
            is SnippetSubmissionResult.InvalidSnippet ->
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
        }
    }

    // US #2 & #4: Actualizar snippet
    @PutMapping("/{id}") // Cambiado a PUT y con ID en path por convención REST
    fun updateSnippet(
        @PathVariable id: UUID,
        @RequestBody newCode: String,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Any> {
        val userId = principal.token.subject
        // Validamos propiedad en el servicio
        val result = snippetService.updateSnippet(id, newCode, userId)

        return when (result) {
            is SnippetSubmissionResult.Success -> ResponseEntity.ok(result)
            is SnippetSubmissionResult.InvalidSnippet ->
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
        }
    }

    // US #13: Descargar Snippet
    @GetMapping("/{id}/download")
    fun downloadSnippet(
        @PathVariable id: UUID,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Resource> {
        val userId = principal.token.subject
        val token = principal.token.tokenValue

        // 1. Obtener Asset Key (validando permisos)
        val assetKey = snippetService.getSnippetForDownload(id, userId, token)

        // 2. Obtener contenido desde Engine
        val code = engineService.getSnippetContent(assetKey)
        val resource = ByteArrayResource(code.toByteArray())

        // 3. Retornar archivo descargable
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"snippet.ps\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(resource.contentLength())
            .body(resource)
    }

    // US #5: Listar snippets (Propios + Compartidos)
    // Sigue el estilo de listAccessible del ejemplo
    @GetMapping
    fun getAllSnippets(
        principal: JwtAuthenticationToken,
        @ModelAttribute filter: SnippetFilterDTO,
    ): ResponseEntity<List<SnippetResponseDTO>> {
        val userId = principal.token.subject
        val snippets =
            snippetService.getAllSnippets(
                userId = userId,
                token = principal.token.tokenValue,
                filter = filter,
            )
        return ResponseEntity.ok(snippets)
    }

    /*
    //US #6: Detalle de un snippet
    @GetMapping("/{id}")
    fun getSnippet(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetResponseDTO> {
        val snippet = snippetService.getSnippetById(id)
        return ResponseEntity.ok(snippet)
    }
     */

    // US #7: Compartir snippet
    @PostMapping("/{id}/share")
    fun shareSnippet(
        @PathVariable id: UUID,
        @RequestBody targetUserId: String,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        val userId = principal.token.subject
        val token = principal.token.tokenValue

        snippetService.shareSnippet(id, targetUserId, userId, token)
        return ResponseEntity.ok().build()
    }

    // US #12: Formatting automatico de snippets
    @PostMapping("/format-all")
    fun formatSnippetAutomatically(principal: JwtAuthenticationToken): ResponseEntity<SnippetResponseDTO> {
        val userId = principal.token.subject
        snippetProcessingService.formatAllSnippets(userId)
        return ResponseEntity.ok().build()
    }

    // US #15: Linting automatico de snippets
    @PostMapping("/lint-all")
    fun lintSnippetAutomatically(principal: JwtAuthenticationToken): ResponseEntity<SnippetResponseDTO> {
        val userId = principal.token.subject
        snippetProcessingService.lintAllSnippets(userId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/format")
    fun formatSnippet(
        principal: JwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = principal.token.subject
        snippetService.formatSnippet(userId, id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/lint")
    fun lintSnippet(
        principal: JwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = principal.token.subject
        snippetService.lintSnippet(userId, id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/asset-key")
    fun getAssetKey(
        @PathVariable id: UUID,
    ): ResponseEntity<String> {
        val assetKey = snippetService.getSnippetAssetKeyById(id)
        return ResponseEntity.ok(assetKey)
    }

    @DeleteMapping("/{id}")
    fun deleteSnippet(
        @PathVariable id: UUID,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        val userId = principal.token.subject
        val token = principal.token.tokenValue
        snippetService.deleteSnippet(id, userId, token)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/metadata")
    fun getSnippetMetadata(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetResponseDTO> {
        val snippet = snippetService.getSnippetById(id)
        return ResponseEntity.ok(snippet)
    }
}
