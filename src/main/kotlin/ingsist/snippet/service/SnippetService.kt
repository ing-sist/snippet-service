package ingsist.snippet.service

import ingsist.snippet.asset.AssetClient
import ingsist.snippet.domain.SnippetMetadata
import ingsist.snippet.domain.ValidationResult
import ingsist.snippet.domain.processEngineResult
import ingsist.snippet.domain.SnippetUploadResult
import ingsist.snippet.domain.SnippetVersion
import ingsist.snippet.dtos.CreateSnippetDTO
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.engine.EngineClient
import ingsist.snippet.repository.SnippetRepository
import ingsist.snippet.repository.SnippetVersionRepository
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val snippetVersionRepository: SnippetVersionRepository,
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

                val exists = snippetRepository.existsById(snippetId)
                if(!exists){
                    // create snippet entity
                    val snippetMetadata = SnippetMetadata(
                        id = snippetId,
                        name = snippet.name,
                        language = snippet.language,
                        description = snippet.description,
                    )
                    // save snippet md
                    snippetRepository.save(snippetMetadata)
                }

                val snippetMetadata = snippetRepository.findById(snippetId).get()

                // save snippet version
                val snippetToSave = SnippetVersion(
                    versionId = UUID.randomUUID(),
                    snippet = snippetMetadata,
                    assetKey = assetKey,
                    createdDate = Date(),
                    versionTag = snippet.versionTag ?: "",
                )

                snippetVersionRepository.save(snippetToSave)

                // result
                return SnippetUploadResult.Success(
                    snippetId = snippetMetadata.id,
                    name = snippetMetadata.name,
                    language = snippetMetadata.language,
                    version = snippet.versionTag ?: "",
                )
            }
        }
    }
}