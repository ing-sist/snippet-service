package ingsist.snippet.service

import ingsist.snippet.asset.AssetService
import ingsist.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.domain.ValidationResult
import ingsist.snippet.domain.processEngineResult
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.dtos.UpdateSnippetDTO
import ingsist.snippet.engine.EngineClient
import ingsist.snippet.repository.SnippetRepository
import ingsist.snippet.repository.SnippetVersionRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID


@Service
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val snippetVersionRepository: SnippetVersionRepository,
    private val assetService: AssetService
) {

    suspend fun processSnippet(snippet: SubmitSnippetDTO): ValidationResult {
        // generate ids
        val snippetId = UUID.randomUUID()
        val assetKey = "snippet-$snippetId.ps"

        // parser
        val request = ExecuteReqDTO(
            snippetId = snippetId,
            content = snippet.code,
            version = snippet.version
        )

        return processEngineResult(EngineClient("/validate").parse(request))
    }


    suspend fun updateSnippet(snippetUpdate: UpdateSnippetDTO, newCode: String): SnippetSubmissionResult {
        // look for existing snippet
        val existingSnippet = snippetRepository.findByName(snippetUpdate.name)
            ?: return SnippetSubmissionResult.InvalidSnippet(
                message = listOf("No snippet found with name ${snippetUpdate.name}"))

        val lastVersion = existingSnippet.versions.last()

        // validate updated snippet
        val snippet = SubmitSnippetDTO(
            code = newCode,
            language = existingSnippet.language,
            version = lastVersion.versionTag,
        )

        val validationResult = processSnippet(snippet) // checks if valid

        when (validationResult) {
            is ValidationResult.Valid -> {
                // valid snippet -> update code
                assetService.update("snippets", lastVersion.assetKey, snippet.code)

                return SnippetSubmissionResult.Success(
                    snippetId = existingSnippet.id,
                    name = existingSnippet.name,
                    language = existingSnippet.language,
                    version = lastVersion.versionTag,
                )
            }

            is ValidationResult.Invalid -> {
                return SnippetSubmissionResult.InvalidSnippet(
                    message = listOf("Snippet update is invalid: ${validationResult.message}")
                )
            }
        }
    }
}