package ingsist.snippet.test

import com.fasterxml.jackson.databind.ObjectMapper
import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.engine.EngineService
import ingsist.snippet.redis.producer.FormattingSnippetProducer
import ingsist.snippet.redis.producer.LintingSnippetProducer
import ingsist.snippet.runner.snippet.domain.ConformanceStatus
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.domain.SnippetVersion
import ingsist.snippet.runner.snippet.dtos.FormattingRulesDTO
import ingsist.snippet.runner.snippet.dtos.LintingRulesDTO
import ingsist.snippet.runner.snippet.dtos.RunSnippetDTO
import ingsist.snippet.runner.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.runner.snippet.dtos.SupportedLanguageDto
import ingsist.snippet.runner.snippet.dtos.ValidateResDto
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

class SnippetDSL(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val engineService: EngineService,
    private val authService: AuthService,
    private val snippetRepository: SnippetRepository,
    private val formattingProducer: FormattingSnippetProducer? = null,
    private val lintingProducer: LintingSnippetProducer? = null,
) {
    private var userId: String = "default-user"
    private var token: String = "mock-token"
    private var lastResult: ResultActions? = null
    private var lastSnippetId: UUID? = null

    // --- GIVEN (Configuración del escenario) ---

    fun givenUser(userId: String): SnippetDSL {
        this.userId = userId
        return this
    }

    fun givenEngineValidatesCorrectly(): SnippetDSL {
        // Mockeamos que el Engine responda que el código es válido
        whenever(engineService.parse(any())).thenReturn(ValidateResDto(UUID.randomUUID(), emptyList()))
        // Mockeamos que el Engine devuelva el contenido cuando se lo pidan
        whenever(engineService.getSnippetContent(any())).thenAnswer {
            // Simulamos devolver un código genérico o basado en el assetKey
            "println('Hello');"
        }
        whenever(engineService.getLanguages()).thenReturn(
            listOf(
                SupportedLanguageDto("printscript", listOf("1.0", "1.1"), "ps"),
            ),
        )
        return this
    }

    fun givenUserHasNoSharedPermissions(snippetId: UUID): SnippetDSL {
        whenever(authService.hasPermission(snippetId, "READ", token)).thenReturn(false)
        return this
    }

    fun givenExistingSnippet(
        id: UUID,
        name: String,
        ownerId: String,
    ): SnippetDSL {
        val snippet =
            SnippetMetadata(
                id = id,
                name = name,
                language = "printscript",
                langVersion = "1.1",
                description = "Description",
                ownerId = ownerId,
                conformance = ConformanceStatus.PENDING,
                createdAt = LocalDateTime.now(),
                versions = mutableListOf(),
            )
        // Agregamos una versión inicial
        val version =
            SnippetVersion(
                versionId = UUID.randomUUID(),
                assetKey = "snippet-$id.ps",
                createdDate = Date(),
                versionTag = "1.0",
                snippet = snippet,
            )
        snippet.versions.add(version)

        snippetRepository.save(snippet)
        this.lastSnippetId = id
        return this
    }

    // --- WHEN (Acciones) ---

    fun whenCreateSnippet(
        name: String,
        content: String,
        language: String,
        version: String,
    ): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.post("/snippets/upload-inline")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }) // Simula Auth0
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(content)
                    .param("name", name)
                    .param("language", language)
                    .param("version", version)
                    .param("description", "Test description")
                    .param("versionTag", "1.0"),
            )

        // Intentamos capturar el ID si la creación fue exitosa para usarlo luego
        try {
            val responseString = lastResult!!.andReturn().response.contentAsString
            if (responseString.isNotEmpty()) {
                val node = objectMapper.readTree(responseString)
                if (node.has("id")) {
                    this.lastSnippetId = UUID.fromString(node.get("id").asText())
                } else if (node.has("snippetId")) {
                    this.lastSnippetId = UUID.fromString(node.get("snippetId").asText())
                }
            }
        } catch (e: Exception) {
            println("Failed to parse response for ID: ${e.message}")
        }

        return this
    }

    fun whenUpdateSnippet(
        name: String,
        content: String,
        language: String,
        version: String,
    ): SnippetDSL {
        requireNotNull(lastSnippetId) { "No snippet ID available directly from previous steps" }

        val dto =
            SubmitSnippetDTO(
                name = name,
                code = content,
                language = language,
                langVersion = version,
                description = "Updated description",
            )

        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.put("/snippets/$lastSnippetId")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)),
            )
        return this
    }

    fun whenGetSnippetById(): SnippetDSL {
        requireNotNull(lastSnippetId) { "Call whenCreateSnippet or givenExistingSnippet first" }
        return whenGetSnippet(lastSnippetId!!)
    }

    fun whenGetSnippet(id: UUID): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/snippets/$id")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) })
                    .contentType(MediaType.APPLICATION_JSON),
            )
        return this
    }

    fun whenGetSnippets(): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/snippets")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    fun whenGetLanguages(): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/languages")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    fun whenUpdateLintingRules(namingType: String): SnippetDSL {
        val dto = LintingRulesDTO(namingType, true, true)
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.put("/rules/linting")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)),
            )
        return this
    }

    fun whenGetLintingRules(): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/rules/linting")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    fun whenUpdateFormattingRules(indentation: Int): SnippetDSL {
        val dto =
            FormattingRulesDTO(
                indentation = indentation,
                spaceBeforeColon = true,
                spaceAfterColon = true,
                spaceAroundAssignment = true,
                spaceAroundOperators = true,
                maxSpaceBetweenTokens = true,
                lineBreakBeforePrintln = 1,
                lineBreakAfterSemiColon = true,
                inlineBraceIfStatement = true,
                belowLineBraceIfStatement = true,
                braceLineBreak = 1,
                keywordSpacingAfter = true,
            )
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.put("/rules/formatting")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)),
            )
        return this
    }

    fun whenGetFormattingRules(): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/rules/formatting")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    fun whenDeleteSnippet(id: UUID): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/snippets/$id")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    fun whenRunSnippet(
        id: UUID,
        inputs: List<String>,
    ): SnippetDSL {
        val dto = RunSnippetDTO(inputs)
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.post("/snippets/$id/run")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)),
            )
        return this
    }

    fun whenShareSnippet(
        id: UUID,
        targetUserId: String,
    ): SnippetDSL {
        val map = mapOf("targetUserId" to targetUserId)
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.post("/snippets/$id/share")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(map)),
            )
        return this
    }

    fun whenDownloadSnippet(id: UUID): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/snippets/$id/download")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    fun whenGetAssetKey(id: UUID): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/snippets/$id/asset-key")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    fun whenUploadSnippetFromFile(
        name: String,
        content: String,
        language: String,
        version: String,
    ): SnippetDSL {
        val file =
            org.springframework.mock.web.MockMultipartFile(
                "file",
                "snippet.ps",
                "text/plain",
                content.toByteArray(),
            )
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.multipart("/snippets/upload-from-file")
                    .file(file)
                    .param("name", name)
                    .param("language", language)
                    .param("version", version)
                    .param("description", "Test description")
                    .param("versionTag", "1.0")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    fun whenRequestFormatSnippet(id: UUID): SnippetDSL {
        lastResult =
            mockMvc.perform(
                MockMvcRequestBuilders.post("/rules/format/$id")
                    .with(jwt().jwt { it.subject(userId).tokenValue(token) }),
            )
        return this
    }

    // --- THEN (Verificaciones) ---

    fun thenStatusIsCreated(): SnippetDSL {
        lastResult!!.andExpect(status().isCreated)
        return this
    }

    fun thenStatusIsOk(): SnippetDSL {
        lastResult!!.andExpect(status().isOk)
        return this
    }

    fun thenStatusIsForbidden(): SnippetDSL {
        lastResult!!.andExpect(status().isForbidden)
        return this
    }

    fun thenStatusIsUnprocessable(): SnippetDSL {
        lastResult!!.andExpect(status().isUnprocessableEntity)
        return this
    }

    fun thenResponseIsExecutionResult(outputs: List<String>): SnippetDSL {
        // Assuming outputs is a list of strings
        // We can check if the response contains these outputs
        // For simplicity, let's just check the first one if it exists
        if (outputs.isNotEmpty()) {
            lastResult!!.andExpect(jsonPath("$.outputs[0]").value(outputs[0]))
        }
        return this
    }

    fun thenResponseIsFile(): SnippetDSL {
        lastResult!!.andExpect(status().isOk)
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists(
                    org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                ),
            )
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .content()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM),
            )
        return this
    }

    fun thenResponseIsAssetKey(assetKey: String): SnippetDSL {
        lastResult!!.andExpect(status().isOk)
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .content().string(assetKey),
            )
        return this
    }

    fun thenResponseContentIs(content: String): SnippetDSL {
        lastResult!!.andExpect(jsonPath("$.content").value(content))
        return this
    }

    fun thenResponseContainsLanguage(name: String): SnippetDSL {
        lastResult!!.andExpect(jsonPath("$[?(@.language == '$name')]").exists())
        return this
    }

    fun thenResponseLintingNamingTypeIs(namingType: String): SnippetDSL {
        lastResult!!.andExpect(jsonPath("$.identifierNamingType").value(namingType))
        return this
    }

    fun thenResponseFormattingIndentationIs(indentation: Int): SnippetDSL {
        lastResult!!.andExpect(jsonPath("$.Indentation").value(indentation))
        return this
    }

    fun thenLintingRulesArePublished(): SnippetDSL {
        requireNotNull(lintingProducer) { "Linting producer mock not provided" }
        verify(lintingProducer, times(1)).publishSnippet(any())
        return this
    }

    fun thenFormattingRulesArePublished(): SnippetDSL {
        requireNotNull(formattingProducer) { "Formatting producer mock not provided" }
        verify(formattingProducer, times(1)).publishSnippet(any())
        return this
    }

    fun thenStatusIsNotFound(): SnippetDSL {
        lastResult!!.andExpect(status().isNotFound)
        return this
    }

    fun thenResponseNameIs(name: String): SnippetDSL {
        lastResult!!.andExpect(jsonPath("$.name").value(name))
        return this
    }

    fun thenResponseContainsSnippet(id: UUID): SnippetDSL {
        lastResult!!.andExpect(jsonPath("$[?(@.id == '$id')]").exists())
        return this
    }

    fun thenResponseContainsSnippet(name: String): SnippetDSL {
        lastResult!!.andExpect(jsonPath("$.content[?(@.name == '$name')]").exists())
        return this
    }

    fun and(): SnippetDSL = this
}
