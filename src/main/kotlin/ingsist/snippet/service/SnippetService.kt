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
import ingsist.snippet.domain.parser.ParserRegistry
import ingsist.snippet.domain.parser.ValidationResult
import ingsist.snippet.domain.snippet.SnippetUploadResult
import ingsist.snippet.dtos.CreateSnippetDTO
import ingsist.snippet.dtos.SnippetParserRequestDTO
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
    private val parserRegistry: ParserRegistry,
    private val assetClient: AssetClient
) {
    suspend fun createSnippet(snippet: CreateSnippetDTO): SnippetUploadResult {
        // parser
        val request = SnippetParserRequestDTO(
            code = snippet.code,
            language = snippet.language,
            version = snippet.version
        )

        val validation = EngineClient(request).parse(request)


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
>>>>>>> 2d4a23d (feat/useCase#1: created snippet controller, service repo & connected to ps parser (not finished yet))
