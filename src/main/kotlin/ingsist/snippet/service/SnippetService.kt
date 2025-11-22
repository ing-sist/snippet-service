package ingsist.snippet.service

import ingsist.snippet.domain.SnippetEntity
import ingsist.snippet.domain.asset.AssetClient
import ingsist.snippet.domain.parser.ParserRegistry
import ingsist.snippet.domain.parser.ValidationResult
import ingsist.snippet.domain.snippet.SnippetSubmissionResult
import ingsist.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.dtos.UpdateSnippetDTO
import ingsist.snippet.repository.SnippetRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val parserRegistry: ParserRegistry,
    private val assetClient: AssetClient
) {
    suspend fun processSnippet(snippet: SubmitSnippetDTO): SnippetSubmissionResult {
        val parser = parserRegistry.getParser(snippet.language, snippet.version)
            ?: return SnippetSubmissionResult.UnsupportedLanguage(
                language = snippet.language,
                version = snippet.version
            )

        // validation
        val validation = parser.validate(snippet.code)

        if (validation is ValidationResult.Invalid) {
            return SnippetSubmissionResult.InvalidSnippet(
                ruleViolated = validation.ruleViolated,
                line = validation.line,
                column = validation.column,
                detail = validation.message
            )
        }
        return SnippetSubmissionResult.ValidatedSnippet(
            name = snippet.name,
            language = snippet.language,
            version = snippet.version,
        )
    }



    suspend fun updateSnippet(snippetUpdate: UpdateSnippetDTO): SnippetSubmissionResult {
        // look for existing snippet
        val existingSnippet = snippetRepository.findByName(snippetUpdate.name)
            ?: return SnippetSubmissionResult.InvalidSnippet(
                ruleViolated = "SnippetNotFound",
                line = 0,
                column = 0,
                detail = "No snippet found with name ${snippetUpdate.name}"
            )

        val code = assetClient.getCode("snippets", existingSnippet.assetKey)

        // validate updated snippet
        val snippet = SubmitSnippetDTO(
            name = snippetUpdate.newName ?: existingSnippet.name,
            code = snippetUpdate.newCode ?: code!!,
            language = snippetUpdate.newLanguage ?: existingSnippet.language,
            version = snippetUpdate.newVersion ?: existingSnippet.version,
            description = snippetUpdate.newDescription ?: existingSnippet.description
        )

        processSnippet(snippet) // checks if valid

        // valid snippet, update fields
        if(snippetUpdate.newCode != null) {
            assetClient.updateCode("snippets", existingSnippet.assetKey, snippet.code)
        }
        if(snippetUpdate.newLanguage != null) { existingSnippet.language = snippet.language }
        if(snippetUpdate.newVersion != null) { existingSnippet.version = snippet.version }
        if(snippetUpdate.newDescription != null) { existingSnippet.description = snippet.description }
        if(snippetUpdate.newName != null) { existingSnippet.name = snippet.name }

        return SnippetSubmissionResult.Success(
            snippetId = existingSnippet.id,
            name = existingSnippet.name,
            language = existingSnippet.language,
            version = existingSnippet.version,
        )
    }
}