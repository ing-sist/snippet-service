package ingsist.snippet.service

import ingsist.snippet.asset.AssetService
import ingsist.snippet.domain.SnippetMetadata
import ingsist.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.domain.SnippetVersion
import ingsist.snippet.domain.ValidationResult
import ingsist.snippet.domain.processEngineResult
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.engine.EngineService
import ingsist.snippet.repository.SnippetRepository
import ingsist.snippet.repository.SnippetVersionRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val snippetVersionRepository: SnippetVersionRepository,
    private val assetService: AssetService,
    private val engineService: EngineService,
) {
    private suspend fun validateSnippet(snippet: ExecuteReqDTO): ValidationResult {
        // parser
        return processEngineResult(engineService.parse(snippet))
    }

    suspend fun updateSnippet(
        snippetName: String,
        newCode: String,
    ): SnippetSubmissionResult {
        // look for existing snippet
        val existingSnippet = snippetRepository.findByName(snippetName)

        return if (existingSnippet == null) {
            SnippetSubmissionResult.InvalidSnippet(
                message = listOf("No snippet found with name $snippetName"),
            )
        } else {
            val lastVersion = existingSnippet.versions.last()

            // validate updated snippet
            val snippet =
                ExecuteReqDTO(
                    code = newCode,
                    language = existingSnippet.language,
                    version = lastVersion.versionTag,
                    snippetId = existingSnippet.id,
                )

            val validationResult = validateSnippet(snippet) // checks if valid

            when (validationResult) {
                is ValidationResult.Valid -> {
                    // valid snippet -> update code
                    assetService.upload("snippets", lastVersion.assetKey, snippet.code)

                    SnippetSubmissionResult.Success(
                        snippetId = existingSnippet.id,
                        name = existingSnippet.name,
                        language = existingSnippet.language,
                        version = lastVersion.versionTag,
                    )
                }

                is ValidationResult.Invalid -> {
                    SnippetSubmissionResult.InvalidSnippet(
                        message = validationResult.message,
                    )
                }
            }
        }
    }

    suspend fun createSnippet(snippet: SubmitSnippetDTO): SnippetSubmissionResult {
        // generate ids
        val snippetId = UUID.randomUUID()
        val assetKey = "snippet-$snippetId.ps"

        val request =
            ExecuteReqDTO(
                code = snippet.code,
                language = snippet.language,
                version = snippet.version,
                snippetId = snippetId,
            )
        val validationResult = validateSnippet(request)

        when (validationResult) {
            is ValidationResult.Invalid -> {
                return SnippetSubmissionResult.InvalidSnippet(
                    message = validationResult.message,
                )
            }
            is ValidationResult.Valid -> {
                // upload snippet to bucket
                assetService.upload("snippets", assetKey, snippet.code)

                val exists = snippetRepository.existsById(snippetId)
                if (!exists) {
                    // create snippet entity
                    val snippetMetadata =
                        SnippetMetadata(
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
                val snippetToSave =
                    SnippetVersion(
                        versionId = UUID.randomUUID(),
                        snippet = snippetMetadata,
                        assetKey = assetKey,
                        createdDate = Date(),
                        versionTag = snippet.versionTag ?: "",
                    )

                snippetVersionRepository.save(snippetToSave)

                // result
                return SnippetSubmissionResult.Success(
                    snippetId = snippetMetadata.id,
                    name = snippetMetadata.name,
                    language = snippetMetadata.language,
                    version = snippet.versionTag ?: "",
                )
            }
        }
    }
}
