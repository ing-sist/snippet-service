package ingsist.snippet.runner.snippet.controller

import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.runner.snippet.dtos.SnippetDetailsDTO
import ingsist.snippet.runner.snippet.dtos.SnippetFilterDTO
import ingsist.snippet.runner.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.runner.snippet.dtos.SnippetUploadDTO
import ingsist.snippet.runner.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.runner.snippet.service.SnippetService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
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
) {
    val log = LoggerFactory.getLogger(SnippetController::class.java)

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
        log.info("Received upload snippet in from file request by user $userId")
        val result = uploadSnippetLogic(code, params, userId, token)
        return result
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
        log.info("Received upload snippet from inline request by user $userId")
        val result = uploadSnippetLogic(code, params, userId, token)
        return result
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
            is SnippetSubmissionResult.Success -> {
                log.info("Snippet created with ID: ${result.snippetId} for user $ownerId")
                ResponseEntity.status(HttpStatus.CREATED).body(result)
            }
            is SnippetSubmissionResult.InvalidSnippet -> {
                log.error("Snippet creation failed for user $ownerId: ${result.message}")
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
            }
        }
    }

    // US #2 & #4: Actualizar snippet
    @PutMapping("/{id}") // Cambiado a PUT y con ID en path por convención REST
    fun updateSnippet(
        @PathVariable id: UUID,
        @RequestBody @Valid snippet: SubmitSnippetDTO,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Any> {
        val userId = principal.token.subject
        log.info("Received update request for snippet ID: $id by user $userId")
        // Validamos propiedad en el servicio
        val result = snippetService.updateSnippet(id, snippet, userId)

        return when (result) {
            is SnippetSubmissionResult.Success -> {
                log.info("Snippet with ID: ${result.snippetId} updated successfully for user $userId")
                ResponseEntity.ok(result)
            }
            is SnippetSubmissionResult.InvalidSnippet -> {
                log.error("Snippet update failed for user $userId: ${result.message}")
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
            }
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

        log.info("Received download request for snippet ID: $id by user $userId")
        // 1. Obtener Asset Key (validando permisos)
        val assetKey = snippetService.getSnippetForDownload(id, userId, token)
        log.debug("Asset Key fetched for snippet ID {}: {}", id, assetKey)
        // 2. Obtener contenido desde Engine
        val code = engineService.getSnippetContent(assetKey)
        val resource = ByteArrayResource(code.toByteArray())

        log.info("Preparing download response...")

        log.info(
            "Successfully prepared download for snippet {} for user {} (assetKey={})",
            id,
            userId,
            assetKey,
        )
        // 3. Retornar archivo descargable (usar assetKey como filename para respetar extensión)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${assetKey}\"")
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
    ): ResponseEntity<Page<SnippetResponseDTO>> {
        val userId = principal.token.subject
        log.info("Received request to list snippets for user $userId with filter $filter")
        val snippets =
            snippetService.getAllSnippets(
                userId = userId,
                token = principal.token.tokenValue,
                filter = filter,
            )
        log.info("getAllSnippets returned {} items for userId={}", snippets.totalElements, userId)
        return ResponseEntity.ok(snippets)
    }

    // US #6: Detalle de un snippet
    @GetMapping("/{id}")
    fun getSnippet(
        @PathVariable id: UUID,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<SnippetDetailsDTO> {
        val userId = principal.token.subject
        log.info("Received request to get details for snippet ID: $id by user $userId")
        val token = principal.token.tokenValue
        val snippet = snippetService.getSnippetById(id, userId, token)
        log.info("Details for snippet ID: $id retrieved successfully for user $userId")
        return ResponseEntity.ok(snippet)
    }

    // US #7: Compartir snippet
    @PostMapping("/{id}/share")
    fun shareSnippet(
        @PathVariable id: UUID,
        @RequestBody targetUserId: Map<String, String>,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        val userId = principal.token.subject
        val token = principal.token.tokenValue
        val targetId = targetUserId["targetUserId"] ?: throw IllegalArgumentException("targetUserId is required")
        log.info("Received request to share snippet ID: $id from user $userId to user $targetId")

        snippetService.shareSnippet(id, targetId, userId, token)
        log.info("Snippet ID: $id shared successfully from user $userId to user $targetId")
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/asset-key")
    fun getAssetKey(
        @PathVariable id: UUID,
    ): ResponseEntity<String> {
        log.debug("Received request to get asset key for snippet ID: {}", id)
        val assetKey = snippetService.getSnippetAssetKeyById(id)
        log.debug("Returning asset key for snippet ID: {}", id)
        return ResponseEntity.ok(assetKey)
    }

    @DeleteMapping("/{id}")
    fun deleteSnippet(
        @PathVariable id: UUID,
        principal: JwtAuthenticationToken,
    ): ResponseEntity<Void> {
        log.info("Received request to delete snippet ID: $id")
        val userId = principal.token.subject
        val token = principal.token.tokenValue
        snippetService.deleteSnippet(id, userId, token)
        log.info("Snippet ID: $id deleted successfully")
        return ResponseEntity.ok().build()
    }
}
