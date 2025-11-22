package ingsist.snippet.controller

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

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val snippetService: SnippetService
) {

    @PostMapping("/update")
    suspend fun updateSnippet(
        @RequestParam("snippetToUpdateName") name: String,
        @RequestParam("newContentFile") newContentFile: MultipartFile?,
        @RequestParam("newName") newName: String?,
        @RequestParam("newLanguage") newLanguage: String?,
        @RequestParam("newVersion") newVersion: String?,
        @RequestParam("newDescription") newDescription: String?,
    ) : ResponseEntity<Any> {
        val code = newContentFile?.bytes?.toString(Charsets.UTF_8)
        val snippetUpdate = UpdateSnippetDTO(
            name = name,
            newName = newName,
            newCode = code,
            newLanguage = newLanguage,
            newVersion = newVersion,
            newDescription = newDescription
        )

        val result = snippetService.updateSnippet(snippetUpdate)

        return when (result) {
            is SnippetSubmissionResult.Success -> ResponseEntity.status(HttpStatus.OK).body(result)
            is SnippetSubmissionResult.InvalidSnippet -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
            is SnippetSubmissionResult.UnsupportedLanguage -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result)
            is SnippetSubmissionResult.ValidatedSnippet -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result)
        }
    }


}