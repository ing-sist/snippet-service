package ingsist.snippet.controller

import ingsist.snippet.domain.SnippetUploadResult
import ingsist.snippet.domain.snippet.SnippetSubmissionResult
import ingsist.snippet.dtos.UpdateSnippetDTO
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

    @PostMapping("/update-from-file")
    suspend fun updateSnippetFromFile(
        @Valid params: UpdateSnippetDTO,
        @RequestParam("newCodeFile") newCodeFile: MultipartFile?,
    ) : ResponseEntity<Any> {
        val code = newCodeFile?.bytes?.toString(Charsets.UTF_8)
        return updateSnippetInline(params, code)
    }

    @PostMapping("/update-inline")
    suspend fun updateSnippetInline(
        @Valid params: UpdateSnippetDTO,
        @RequestParam("newCode") newCode: String?,
    ) : ResponseEntity<Any> {
        return updateSnippetLogic(params, newCode)
    }

    private suspend fun updateSnippetLogic(params: UpdateSnippetDTO, code: String?): ResponseEntity<Any> {
        val result = snippetService.updateSnippet(params)
        return when (result) {
            is SnippetSubmissionResult.Success -> ResponseEntity.status(HttpStatus.OK).body(result)
            is SnippetSubmissionResult.InvalidSnippet -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
            is SnippetSubmissionResult.UnsupportedLanguage -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result)
            is SnippetSubmissionResult.ValidatedSnippet -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result)
        }
    }
}