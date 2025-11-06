package ingsist.snippet.service

import ingsist.snippet.domain.SnippetEntity
import ingsist.snippet.domain.asset.AssetClient
import ingsist.snippet.domain.parser.ParserRegistry
import ingsist.snippet.domain.parser.ValidationResult
import ingsist.snippet.domain.snippet.SnippetUploadResult
import ingsist.snippet.dtos.CreateSnippetDTO
import ingsist.snippet.repository.SnippetRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val parserRegistry: ParserRegistry,
    private val assetClient: AssetClient
) {
    suspend fun createSnippet(snippet: CreateSnippetDTO): SnippetUploadResult {
        // parser
        val parser = parserRegistry.getParser(snippet.language, snippet.version)
            ?: return SnippetUploadResult.UnsupportedLanguage(
                language = snippet.language,
                version = snippet.version
            )

        // validation
        val validation = parser.validate(snippet.code)

        if (validation is ValidationResult.Invalid) {
            return SnippetUploadResult.InvalidSnippet(
                ruleViolated = validation.ruleViolated,
                line = validation.line,
                column = validation.column,
                detail = validation.message
            )
        }

        // generate ids
        val snippetId = UUID.randomUUID().toString()
        val assetKey = "snippet-$snippetId.ps"

        // upload snippet to bucket
        assetClient.upload("snippets", assetKey, snippet.code)

        // save snippet to db
        val snippetToSave = SnippetEntity(
            id = snippetId,
            name = snippet.name,
            language = snippet.language,
            version = snippet.version,
            description = snippet.description,
            assetKey = assetKey
        )

        val savedSnippet = snippetRepository.saveSnippet(snippetToSave)

        // result
        return SnippetUploadResult.Success(
            snippetId = savedSnippet.id,
            name = savedSnippet.name,
            language = savedSnippet.language,
            version = savedSnippet.version,
        )
    }
}