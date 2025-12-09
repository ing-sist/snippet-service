package ingsist.snippet.test

import com.fasterxml.jackson.databind.ObjectMapper
import ingsist.snippet.SnippetApplication
import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.engine.EngineService
import ingsist.snippet.redis.producer.FormattingSnippetProducer
import ingsist.snippet.redis.producer.LintingSnippetProducer
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest(classes = [SnippetApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SnippetIntegrationTest {
    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var snippetRepository: SnippetRepository

    // Mocks de servicios externos (lo que está fuera de nuestro dominio)
    @MockBean private lateinit var engineService: EngineService

    @MockBean private lateinit var authService: AuthService

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtDecoder: JwtDecoder

    // Mocks de Redis para evitar conexión real
    @MockBean private lateinit var formattingProducer: FormattingSnippetProducer

    @MockBean private lateinit var lintingProducer: LintingSnippetProducer

    @Test
    fun `should create and retrieve a snippet successfully`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenEngineValidatesCorrectly()
            .whenCreateSnippet(
                name = "Test Snippet",
                content = "println('Hello');",
                language = "printscript",
                version = "1.1",
            )
            .thenStatusIsCreated()
            .thenResponseNameIs("Test Snippet")
            .and() // Chain para continuar con otra operación
            .whenGetSnippetById()
            .thenStatusIsOk()
            .thenResponseContentIs("println('Hello');")
    }

    @Test
    fun `should fail when creating snippet with unsupported language`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .whenCreateSnippet(
                name = "Bad Snippet",
                content = "code",
                language = "unsupported-lang",
                version = "1.0",
            )
            .thenStatusIsUnprocessable()
    }

    @Test
    fun `should update snippet and generate new version`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenEngineValidatesCorrectly()
            .givenExistingSnippet(
                id = UUID.randomUUID(),
                name = "Old Name",
                ownerId = "user-1",
            )
            .whenUpdateSnippet(
                name = "New Name",
                content = "println('Updated');",
                language = "printscript",
                version = "1.1",
            )
            .thenStatusIsOk()
            .thenResponseNameIs("New Name")
    }

    @Test
    fun `should forbid access to snippet owned by another user without permission`() {
        val otherUserId = "user-2"
        val snippetId = UUID.randomUUID()

        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1") // Soy user-1 y other user es el dueño, es user-2
            .givenExistingSnippet(
                id = snippetId,
                name = "Other's snippet",
                ownerId = otherUserId,
            )
            .givenUserHasNoSharedPermissions(snippetId)
            .whenGetSnippet(snippetId)
            .thenStatusIsForbidden()
    }

    @Test
    fun `should retrieve supported languages`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenEngineValidatesCorrectly()
            .whenGetLanguages()
            .thenStatusIsOk()
            .thenResponseContainsLanguage("printscript")
    }

    @Test
    fun `should update and retrieve linting rules`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenExistingSnippet(UUID.randomUUID(), "Test Snippet", "user-1")
            .whenUpdateLintingRules("camelCase")
            .thenStatusIsOk()
            .thenLintingRulesArePublished()
            .and()
            .whenGetLintingRules()
            .thenStatusIsOk()
            .thenResponseLintingNamingTypeIs("camelCase")
    }

    @Test
    fun `should update and retrieve formatting rules`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenExistingSnippet(UUID.randomUUID(), "Test Snippet", "user-1")
            .whenUpdateFormattingRules(4)
            .thenStatusIsOk()
            .thenFormattingRulesArePublished()
            .and()
            .whenGetFormattingRules()
            .thenStatusIsOk()
            .thenResponseFormattingIndentationIs(4)
    }

    @Test
    fun `should return not found when getting non-existent snippet`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .whenGetSnippet(UUID.randomUUID())
            .thenStatusIsNotFound()
    }

    @Test
    fun `should delete snippet`() {
        val snippetId = UUID.randomUUID()
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenExistingSnippet(snippetId, "ToDelete", "user-1")
            .whenDeleteSnippet(snippetId)
            .thenStatusIsOk()
            .whenGetSnippet(snippetId)
            .thenStatusIsNotFound()
    }

    @Test
    fun `should run snippet`() {
        val snippetId = UUID.randomUUID()
        // Mock engine execution
        org.mockito.kotlin.whenever(engineService.execute(org.mockito.kotlin.any())).thenReturn(
            ingsist.snippet.runner.snippet.dtos.ExecuteResDTO(snippetId, listOf("Hello World"), emptyList()),
        )

        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenExistingSnippet(snippetId, "ToRun", "user-1")
            .whenRunSnippet(snippetId, listOf())
            .thenStatusIsOk()
            .thenResponseIsExecutionResult(listOf("Hello World"))
    }

    @Test
    fun `should list snippets`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenExistingSnippet(UUID.randomUUID(), "Snippet1", "user-1")
            .whenGetSnippets()
            .thenStatusIsOk()
            .thenResponseContainsSnippet("Snippet1")
    }

    @Test
    fun `should share snippet`() {
        val snippetId = UUID.randomUUID()
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenExistingSnippet(snippetId, "ToShare", "user-1")
            .whenShareSnippet(snippetId, "user-2")
            .thenStatusIsOk()

        org.mockito.kotlin.verify(authService).grantPermission(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }

    @Test
    fun `should download snippet`() {
        val snippetId = UUID.randomUUID()
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenEngineValidatesCorrectly()
            .givenExistingSnippet(snippetId, "ToDownload", "user-1")
            .whenDownloadSnippet(snippetId)
            .thenResponseIsFile()
    }

    @Test
    fun `should get asset key`() {
        val snippetId = UUID.randomUUID()
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenExistingSnippet(snippetId, "ToGetAssetKey", "user-1")
            .whenGetAssetKey(snippetId)
            .thenResponseIsAssetKey("snippet-$snippetId.ps")
    }

    @Test
    fun `should upload snippet from file`() {
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenEngineValidatesCorrectly()
            .whenUploadSnippetFromFile("FromFile", "println('file')", "printscript", "1.0")
            .thenStatusIsCreated()
            .thenResponseNameIs("FromFile")
    }

    @Test
    fun `should request format snippet`() {
        val snippetId = UUID.randomUUID()
        SnippetDSL(
            mockMvc,
            objectMapper,
            engineService,
            authService,
            snippetRepository,
            formattingProducer,
            lintingProducer,
        )
            .givenUser("user-1")
            .givenExistingSnippet(snippetId, "ToFormat", "user-1")
            .whenRequestFormatSnippet(snippetId)
            .thenStatusIsOk()
            .thenFormattingRulesArePublished()
    }
}
