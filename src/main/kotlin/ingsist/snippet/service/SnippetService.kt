package ingsist.snippet.service

<<<<<<< HEAD
import ingsist.snippet.asset.AssetService
import ingsist.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.domain.ValidationResult
import ingsist.snippet.domain.processEngineResult
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.dtos.UpdateSnippetDTO
import ingsist.snippet.engine.EngineService
import ingsist.snippet.repository.SnippetRepository
import jakarta.transaction.Transactional
=======
import ingsist.snippet.domain.SnippetEntity
import ingsist.snippet.asset.AssetClient
import ingsist.snippet.domain.ValidationResult
import ingsist.snippet.domain.processEngineResult
import ingsist.snippet.domain.SnippetUploadResult
import ingsist.snippet.dtos.CreateSnippetDTO
import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.engine.EngineClient
import ingsist.snippet.repository.SnippetRepository
>>>>>>> 2d4a23d (feat/useCase#1: created snippet controller, service repo & connected to ps parser (not finished yet))
import org.springframework.stereotype.Service
import java.util.UUID

@Service
<<<<<<< HEAD
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val assetService: AssetService,
    private val engineService: EngineService,
) {
    suspend fun processSnippet(snippet: SubmitSnippetDTO): ValidationResult {
        // generate ids
        val snippetId = UUID.randomUUID()

        // parser
        val request =
            ExecuteReqDTO(
                snippetId = snippetId,
                content = snippet.code,
                version = snippet.version,
            )

        return processEngineResult(engineService.parse(request))
    }

    suspend fun updateSnippet(
        snippetUpdate: UpdateSnippetDTO,
        newCode: String,
    ): SnippetSubmissionResult {
        // look for existing snippet
        val existingSnippet =
            snippetRepository.findByName(snippetUpdate.name)
                ?: return SnippetSubmissionResult.InvalidSnippet(
                    message = listOf("No snippet found with name ${snippetUpdate.name}"),
                )

        val lastVersion = existingSnippet.versions.last()

        // validate updated snippet
        val snippet =
            SubmitSnippetDTO(
                code = newCode,
                language = existingSnippet.language,
                version = lastVersion.versionTag,
            )

        val validationResult = processSnippet(snippet) // checks if valid

        return when (validationResult) {
            is ValidationResult.Valid -> {
                // valid snippet -> update code
                assetService.update("snippets", lastVersion.assetKey, snippet.code)

                SnippetSubmissionResult.Success(
                    snippetId = existingSnippet.id,
                    name = existingSnippet.name,
                    language = existingSnippet.language,
                    version = lastVersion.versionTag,
                )
            }

            is ValidationResult.Invalid -> {
                SnippetSubmissionResult.InvalidSnippet(
                    message = listOf("Snippet update is invalid: ${validationResult.message}"),
                )
            }
        }
    }
}
=======
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
>>>>>>> 2d4a23d (feat/useCase#1: created snippet controller, service repo & connected to ps parser (not finished yet))
