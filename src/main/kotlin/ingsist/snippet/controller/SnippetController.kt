package ingsist.snippet.controller

import ingsist.snippet.domain.snippet.SnippetUploadResult
import ingsist.snippet.dtos.CreateSnippetDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ingsist.snippet.service.SnippetService

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val snippetService: SnippetService
) {
    @PostMapping("/upload")
    suspend fun uploadSnippet(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("name") name: String,
        @RequestParam("language") language: String,
        @RequestParam("version") version: String,
        @RequestParam("description") description: String
    ) : ResponseEntity<Any> {
        val code = file.bytes.toString(Charsets.UTF_8)
        val snippet = CreateSnippetDTO(code, name, language, version, description)
        val result = snippetService.createSnippet(snippet)
        return when (result) {
            is SnippetUploadResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result)
            is SnippetUploadResult.InvalidSnippet -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
            is SnippetUploadResult.UnsupportedLanguage -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result)
        }
    }


}