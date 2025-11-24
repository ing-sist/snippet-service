package ingsist.snippet.controller

import ingsist.snippet.domain.SnippetUploadResult
import ingsist.snippet.dtos.CreateSnippetDTO
import ingsist.snippet.dtos.SnippetUploadDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ingsist.snippet.service.SnippetService
import jakarta.validation.Valid

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val snippetService: SnippetService
) {
    @PostMapping("/upload-from-file")
    suspend fun uploadSnippetFromFile(
        @RequestParam("file") file: MultipartFile,
        @Valid params: SnippetUploadDTO
    ) : ResponseEntity<Any> {
        val code = file.bytes.toString(Charsets.UTF_8)
        return uploadSnippetLogic(code, params)
    }

    @PostMapping("/upload-inline")
    suspend fun uploadSnippetInline(
        @RequestParam("code") code: String,
        @Valid params: SnippetUploadDTO
    ) : ResponseEntity<Any> {
        return uploadSnippetLogic(code, params)
    }

    private suspend fun uploadSnippetLogic(
        code: String,
        params: SnippetUploadDTO
    ) : ResponseEntity<Any> {
        val snippet = CreateSnippetDTO(code, params.name, params.language,
            params.version, params.description)
        val result = snippetService.createSnippet(snippet)
        return resultHandler(result)
    }

    private suspend fun resultHandler(result: SnippetUploadResult): ResponseEntity<Any> {
        return when (result) {
            is SnippetUploadResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result)
            is SnippetUploadResult.InvalidSnippet -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
        }
    }
}