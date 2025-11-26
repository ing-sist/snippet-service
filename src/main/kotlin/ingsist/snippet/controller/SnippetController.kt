package ingsist.snippet.controller

<<<<<<< HEAD
import ingsist.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.dtos.UpdateSnippetDTO
import ingsist.snippet.service.SnippetService
import jakarta.validation.Valid
=======
import ingsist.snippet.domain.snippet.SnippetUploadResult
import ingsist.snippet.dtos.CreateSnippetDTO
>>>>>>> 2d4a23d (feat/useCase#1: created snippet controller, service repo & connected to ps parser (not finished yet))
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
<<<<<<< HEAD
=======
import ingsist.snippet.service.SnippetService
>>>>>>> 2d4a23d (feat/useCase#1: created snippet controller, service repo & connected to ps parser (not finished yet))

@RestController
@RequestMapping("/snippets")
class SnippetController(
<<<<<<< HEAD
    private val snippetService: SnippetService,
) {
    @PostMapping("/update-from-file")
    suspend fun updateSnippetFromFile(
        @Valid params: UpdateSnippetDTO,
        @RequestParam("newCodeFile") newCodeFile: MultipartFile,
    ): ResponseEntity<Any> {
        val code = newCodeFile.bytes.toString(Charsets.UTF_8)
        return updateSnippetInline(params, code)
    }

    @PostMapping("/update-inline")
    suspend fun updateSnippetInline(
        @Valid params: UpdateSnippetDTO,
        @RequestParam("newCode") newCode: String,
    ): ResponseEntity<Any> {
        return updateSnippetLogic(params, newCode)
    }

    private suspend fun updateSnippetLogic(
        params: UpdateSnippetDTO,
        code: String,
    ): ResponseEntity<Any> {
        val result = snippetService.updateSnippet(params, code)
        return resultHandler(result)
    }

    private suspend fun resultHandler(result: SnippetSubmissionResult): ResponseEntity<Any> {
        return when (result) {
            is SnippetSubmissionResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result)
            is SnippetSubmissionResult.InvalidSnippet ->
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result)
        }
    }
}
=======
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
>>>>>>> 2d4a23d (feat/useCase#1: created snippet controller, service repo & connected to ps parser (not finished yet))
