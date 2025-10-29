package service

import domain.parser.ParserRegistry
import domain.parser.ValidationResult
import domain.snippet.SnippetUploadResult
import dtos.CreateSnippetDTO
import dtos.Snippet
import repository.SnippetRepository
import java.util.UUID


class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val parserRegistry: ParserRegistry
) {
    fun createSnippet(snippet: CreateSnippetDTO): SnippetUploadResult {
        val parser = parserRegistry.getParser(snippet.language, snippet.version)
            ?: return SnippetUploadResult.UnsupportedLanguage(
                language = snippet.language,
                version = snippet.version
            )

        val validation = parser.validate(snippet.code)

        if (validation is ValidationResult.Invalid) {
            return SnippetUploadResult.InvalidSnippet(
                ruleViolated = validation.ruleViolated,
                line = validation.line,
                column = validation.column,
                detail = validation.message
            )
        }

        val snippetToSave = Snippet(
            id = UUID.randomUUID().toString(),
            name = snippet.name,
            language = snippet.language,
            version = snippet.version,
            code = snippet.code
        )

        val savedSnippet = snippetRepository.saveSnippet(snippetToSave)
        return SnippetUploadResult.Success(
            snippetId = savedSnippet.id,
            name = savedSnippet.name,
            language = savedSnippet.language,
            version = savedSnippet.version
        )
    }
}