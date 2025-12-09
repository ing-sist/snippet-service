package ingsist.snippet.runner.snippet.service

import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.dtos.SnippetFilterDTO
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SnippetServiceListTest {
    @Mock
    private lateinit var snippetRepository: SnippetRepository

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var snippetVersionRepository: SnippetVersionRepository

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var engineService: EngineService

    @Mock
    private lateinit var permissionService: PermissionService

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var languageService: LanguageService

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var rulesService: RulesService

    @InjectMocks
    private lateinit var snippetService: SnippetService

    private fun <T> any(type: Class<T>): T = Mockito.any(type)

    private fun anySpec(): Specification<SnippetMetadata>? = Mockito.any()

    @Test
    fun `should list owned snippets`() {
        val userId = "user1"
        val token = "token"
        val filter = SnippetFilterDTO(mode = "OWNED")

        val page = PageImpl(listOf<SnippetMetadata>())
        Mockito.`when`(snippetRepository.findAll(anySpec(), any(Pageable::class.java))).thenReturn(page)

        val result = snippetService.getAllSnippets(userId, token, filter)

        assertEquals(0, result.totalElements)
        Mockito.verify(permissionService, Mockito.never())
            .getSharedSnippetIds(any(String::class.java), any(String::class.java))
    }

    @Test
    fun `should list shared snippets`() {
        val userId = "user1"
        val token = "token"
        val filter = SnippetFilterDTO(mode = "SHARED")
        val sharedId = UUID.randomUUID()

        Mockito.`when`(permissionService.getSharedSnippetIds(userId, token)).thenReturn(listOf(sharedId))

        val page = PageImpl(listOf<SnippetMetadata>())
        Mockito.`when`(snippetRepository.findAll(anySpec(), any(Pageable::class.java))).thenReturn(page)

        val result = snippetService.getAllSnippets(userId, token, filter)

        assertEquals(0, result.totalElements)
    }

    @Test
    fun `should return empty page when mode is SHARED and no shared snippets`() {
        val userId = "user1"
        val token = "token"
        val filter = SnippetFilterDTO(mode = "SHARED")

        Mockito.`when`(permissionService.getSharedSnippetIds(userId, token)).thenReturn(emptyList())

        val result = snippetService.getAllSnippets(userId, token, filter)

        assertEquals(0, result.totalElements)
        Mockito.verify(snippetRepository, Mockito.never()).findAll(anySpec(), any(Pageable::class.java))
    }

    @Test
    fun `should list all snippets (owned + shared)`() {
        val userId = "user1"
        val token = "token"
        val filter = SnippetFilterDTO(mode = "ALL")
        val sharedId = UUID.randomUUID()

        Mockito.`when`(permissionService.getSharedSnippetIds(userId, token)).thenReturn(listOf(sharedId))

        val page = PageImpl(listOf<SnippetMetadata>())
        Mockito.`when`(snippetRepository.findAll(anySpec(), any(Pageable::class.java))).thenReturn(page)

        val result = snippetService.getAllSnippets(userId, token, filter)

        assertEquals(0, result.totalElements)
    }

    @Test
    fun `should list all snippets when no shared snippets`() {
        val userId = "user1"
        val token = "token"
        val filter = SnippetFilterDTO(mode = "ALL")

        Mockito.`when`(permissionService.getSharedSnippetIds(userId, token)).thenReturn(emptyList())

        val page = PageImpl(listOf<SnippetMetadata>())
        Mockito.`when`(snippetRepository.findAll(anySpec(), any(Pageable::class.java))).thenReturn(page)

        val result = snippetService.getAllSnippets(userId, token, filter)

        assertEquals(0, result.totalElements)
    }

    @Test
    fun `should filter by name`() {
        val userId = "user1"
        val token = "token"
        val filter = SnippetFilterDTO(name = "test")

        val page = PageImpl(listOf<SnippetMetadata>())
        Mockito.`when`(snippetRepository.findAll(anySpec(), any(Pageable::class.java))).thenReturn(page)

        snippetService.getAllSnippets(userId, token, filter)

        Mockito.verify(snippetRepository).findAll(anySpec(), any(Pageable::class.java))
    }

    @Test
    fun `should filter by language`() {
        val userId = "user1"
        val token = "token"
        val filter = SnippetFilterDTO(language = "printscript")

        val page = PageImpl(listOf<SnippetMetadata>())
        Mockito.`when`(snippetRepository.findAll(anySpec(), any(Pageable::class.java))).thenReturn(page)

        snippetService.getAllSnippets(userId, token, filter)

        Mockito.verify(snippetRepository).findAll(anySpec(), any(Pageable::class.java))
    }

    @Test
    fun `should filter by conformance`() {
        val userId = "user1"
        val token = "token"
        val filter = SnippetFilterDTO(conformance = "COMPLIANT")

        val page = PageImpl(listOf<SnippetMetadata>())
        Mockito.`when`(snippetRepository.findAll(anySpec(), any(Pageable::class.java))).thenReturn(page)

        snippetService.getAllSnippets(userId, token, filter)

        Mockito.verify(snippetRepository).findAll(anySpec(), any(Pageable::class.java))
    }
}
