package service

import domain.asset.AssetClient
import domain.parser.ParserRegistry
import domain.parser.ValidationResult
import domain.snippet.SnippetUploadResult
import dtos.CreateSnippetDTO
import dtos.SnippetMetaData
import repository.SnippetRepository
import java.util.UUID


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
        val snippetToSave = SnippetMetaData(
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