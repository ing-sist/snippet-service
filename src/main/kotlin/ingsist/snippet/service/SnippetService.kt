package ingsist.snippet.service

import ingsist.snippet.domain.SnippetEntity
import ingsist.snippet.asset.AssetClient
import ingsist.snippet.domain.ValidationResult
import ingsist.snippet.domain.processEngineResult
import ingsist.snippet.domain.SnippetUploadResult
import ingsist.snippet.dtos.CreateSnippetDTO
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.engine.EngineClient
import ingsist.snippet.repository.SnippetRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val assetClient: AssetClient
) {
    suspend fun createSnippet(snippet: CreateSnippetDTO): SnippetUploadResult {
        // generate ids
        val snippetId = UUID.randomUUID()
        val assetKey = "snippet-$snippetId.ps"

        // parser
        val request = ExecuteReqDTO(
            snippetId = snippetId,
            content = snippet.code,
            version = snippet.version
        )

        val validationResult = processEngineResult(EngineClient("/validate").parse(request))

        when (validationResult) {
            is ValidationResult.Invalid -> {
                return SnippetUploadResult.InvalidSnippet(
                    message = validationResult.message,
                )
            }
            is ValidationResult.Valid -> {
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

                val savedSnippet = snippetRepository.save(snippetToSave)

                // result
                return SnippetUploadResult.Success(
                    snippetId = savedSnippet.id,
                    name = savedSnippet.name,
                    language = savedSnippet.language,
                    version = savedSnippet.version,
                )
            }
        }
    }
}