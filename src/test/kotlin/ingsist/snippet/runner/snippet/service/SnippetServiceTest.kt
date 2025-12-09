package ingsist.snippet.runner.snippet.service

import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import ingsist.snippet.runner.snippet.domain.SnippetSubmissionResult
import ingsist.snippet.runner.snippet.domain.SnippetVersion
import ingsist.snippet.runner.snippet.dtos.SubmitSnippetDTO
import ingsist.snippet.runner.snippet.dtos.ValidateReqDto
import ingsist.snippet.runner.snippet.dtos.ValidateResDto
import ingsist.snippet.runner.snippet.repository.SnippetRepository
import ingsist.snippet.runner.snippet.repository.SnippetVersionRepository
import ingsist.snippet.shared.exception.ExternalServiceException
import ingsist.snippet.shared.exception.SnippetAccessDeniedException
import ingsist.snippet.shared.exception.SnippetNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.Date
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SnippetServiceTest {
    private fun <T> any(type: Class<T>): T = Mockito.any(type)

    private fun <T> any(): T = Mockito.any()

    private fun anyValidateReqDto(): ValidateReqDto {
        Mockito.any(ValidateReqDto::class.java)
        return ValidateReqDto(UUID.randomUUID(), "", "", "", "")
    }

    private fun anySnippetMetadata(): SnippetMetadata {
        Mockito.any(SnippetMetadata::class.java)
        return SnippetMetadata(UUID.randomUUID(), "", "", "", "", "")
    }

    @Mock
    private lateinit var snippetRepository: SnippetRepository

    @Mock
    private lateinit var snippetVersionRepository: SnippetVersionRepository

    @Mock
    private lateinit var engineService: EngineService

    @Mock
    private lateinit var permissionService: PermissionService

    @Mock
    private lateinit var languageService: LanguageService

    @Suppress("UnusedPrivateProperty")
    @Mock
    private lateinit var rulesService: RulesService

    @InjectMocks
    private lateinit var snippetService: SnippetService

    @Test
    fun `updateSnippet should return invalid when snippet not found`() {
        val snippetId = UUID.randomUUID()
        val userId = "user1"
        val dto = SubmitSnippetDTO("name", "code", "printscript", "1.0", "desc", "1.0")

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        val result = snippetService.updateSnippet(snippetId, dto, userId)

        assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
        assertEquals("Snippet not found", (result as SnippetSubmissionResult.InvalidSnippet).message[0])
    }

    @Test
    fun `updateSnippet should return invalid when user is not owner`() {
        val snippetId = UUID.randomUUID()
        val userId = "user1"
        val ownerId = "user2"
        val dto = SubmitSnippetDTO("name", "code", "printscript", "1.0", "desc", "1.0")
        val metadata = SnippetMetadata(snippetId, "name", "printscript", "1.0", "desc", ownerId)

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))

        val result = snippetService.updateSnippet(snippetId, dto, userId)

        assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
        assertEquals(
            "You are not the owner of this snippet",
            (result as SnippetSubmissionResult.InvalidSnippet).message[0],
        )
    }

    @Test
    fun `updateSnippet should return invalid when language not supported`() {
        val snippetId = UUID.randomUUID()
        val userId = "user1"
        val dto = SubmitSnippetDTO("name", "code", "unsupported", "1.0", "desc", "1.0")
        val metadata = SnippetMetadata(snippetId, "name", "printscript", "1.0", "desc", userId)

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))
        Mockito.`when`(languageService.isLanguageSupported("unsupported", "1.0")).thenReturn(false)

        val result = snippetService.updateSnippet(snippetId, dto, userId)

        assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
        assertTrue((result as SnippetSubmissionResult.InvalidSnippet).message[0].contains("not supported"))
    }

    @Test
    fun `updateSnippet should return invalid when engine validation fails`() {
        val snippetId = UUID.randomUUID()
        val userId = "user1"
        val dto = SubmitSnippetDTO("name", "code", "printscript", "1.0", "desc", "1.0")
        val metadata = SnippetMetadata(snippetId, "name", "printscript", "1.0", "desc", userId)
        metadata.versions.add(SnippetVersion(UUID.randomUUID(), "key", Date(), "1.0", metadata))

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))
        Mockito.`when`(languageService.isLanguageSupported("printscript", "1.0")).thenReturn(true)
        Mockito.doReturn(ValidateResDto(snippetId, listOf("Syntax error")))
            .`when`(engineService).parse(anyValidateReqDto())

        val result = snippetService.updateSnippet(snippetId, dto, userId)

        assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
    }

    @Test
    fun `updateSnippet should return success when valid`() {
        val snippetId = UUID.randomUUID()
        val userId = "user1"
        val dto = SubmitSnippetDTO("name", "code", "printscript", "1.0", "desc", "1.0")
        val metadata = SnippetMetadata(snippetId, "name", "printscript", "1.0", "desc", userId)
        metadata.versions.add(SnippetVersion(UUID.randomUUID(), "key", Date(), "1.0", metadata))

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))
        Mockito.`when`(languageService.isLanguageSupported("printscript", "1.0")).thenReturn(true)
        Mockito.doReturn(ValidateResDto(snippetId, emptyList())).`when`(engineService).parse(anyValidateReqDto())
        Mockito.`when`(snippetRepository.save(anySnippetMetadata())).thenReturn(metadata)

        val result = snippetService.updateSnippet(snippetId, dto, userId)

        assertTrue(result is SnippetSubmissionResult.Success)
    }

    @Test
    fun `createSnippet should return invalid when language not supported`() {
        val userId = "user1"
        val dto = SubmitSnippetDTO("name", "code", "unsupported", "1.0", "desc", "1.0")

        Mockito.`when`(languageService.isLanguageSupported("unsupported", "1.0")).thenReturn(false)

        val result = snippetService.createSnippet(dto, userId, "token")

        assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
    }

    @Test
    fun `createSnippet should return invalid when engine validation fails`() {
        val userId = "user1"
        val dto = SubmitSnippetDTO("name", "code", "printscript", "1.0", "desc", "1.0")

        Mockito.`when`(languageService.isLanguageSupported("printscript", "1.0")).thenReturn(true)
        Mockito.`when`(languageService.getExtension("printscript")).thenReturn("ps")
        Mockito.doReturn(ValidateResDto(UUID.randomUUID(), listOf("Error")))
            .`when`(engineService).parse(anyValidateReqDto())

        val result = snippetService.createSnippet(dto, userId, "token")

        assertTrue(result is SnippetSubmissionResult.InvalidSnippet)
    }

    @Test
    fun `createSnippet should return success when valid`() {
        val userId = "user1"
        val dto = SubmitSnippetDTO("name", "code", "printscript", "1.0", "desc", "1.0")

        Mockito.`when`(languageService.isLanguageSupported("printscript", "1.0")).thenReturn(true)
        Mockito.`when`(languageService.getExtension("printscript")).thenReturn("ps")
        Mockito.doReturn(ValidateResDto(UUID.randomUUID(), emptyList()))
            .`when`(engineService).parse(anyValidateReqDto())
        Mockito.`when`(snippetRepository.save(anySnippetMetadata())).thenAnswer { it.arguments[0] }

        val result = snippetService.createSnippet(dto, userId, "token")

        assertTrue(result is SnippetSubmissionResult.Success)
    }

    @Test
    fun `deleteSnippet should throw exception when snippet not found`() {
        val snippetId = UUID.randomUUID()
        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        assertThrows(SnippetNotFoundException::class.java) {
            snippetService.deleteSnippet(snippetId, "user1", "token")
        }
    }

    @Test
    fun `deleteSnippet should throw exception when user not owner`() {
        val snippetId = UUID.randomUUID()
        val metadata = SnippetMetadata(snippetId, "name", "ps", "1.0", "desc", "owner")
        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))

        assertThrows(SnippetAccessDeniedException::class.java) {
            snippetService.deleteSnippet(snippetId, "other", "token")
        }
    }

    @Test
    fun `deleteSnippet should handle engine deletion failure`() {
        val snippetId = UUID.randomUUID()
        val metadata = SnippetMetadata(snippetId, "name", "ps", "1.0", "desc", "owner")
        val version = SnippetVersion(UUID.randomUUID(), "key", Date(), "1.0", metadata)

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))
        Mockito.`when`(
            snippetVersionRepository.findLatestBySnippetId(any(UUID::class.java), any(Pageable::class.java)),
        ).thenReturn(PageImpl(listOf(version)))
        Mockito.doThrow(ExternalServiceException("Engine error")).`when`(engineService).deleteSnippet("key")

        assertThrows(ExternalServiceException::class.java) {
            snippetService.deleteSnippet(snippetId, "owner", "token")
        }
    }

    @Test
    fun `deleteSnippet should handle permission deletion failure`() {
        val snippetId = UUID.randomUUID()
        val metadata = SnippetMetadata(snippetId, "name", "ps", "1.0", "desc", "owner")
        val version = SnippetVersion(UUID.randomUUID(), "key", Date(), "1.0", metadata)

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))
        Mockito.`when`(
            snippetVersionRepository.findLatestBySnippetId(any(UUID::class.java), any(Pageable::class.java)),
        ).thenReturn(PageImpl(listOf(version)))
        Mockito.doThrow(ExternalServiceException("Auth error")).`when`(permissionService)
            .deleteSnippetPermissions(snippetId, "token")

        // Should not throw exception, just log warning
        snippetService.deleteSnippet(snippetId, "owner", "token")

        Mockito.verify(snippetRepository).delete(metadata)
    }

    @Test
    fun `getSnippetForDownload should throw exception when snippet not found`() {
        val snippetId = UUID.randomUUID()
        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        assertThrows(SnippetNotFoundException::class.java) {
            snippetService.getSnippetForDownload(snippetId, "user1", "token")
        }
    }

    @Test
    fun `getSnippetForDownload should throw exception when access denied`() {
        val snippetId = UUID.randomUUID()
        val metadata = SnippetMetadata(snippetId, "name", "ps", "1.0", "desc", "owner")
        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))
        Mockito.`when`(permissionService.hasReadPermission(snippetId, "token")).thenReturn(false)

        assertThrows(SnippetAccessDeniedException::class.java) {
            snippetService.getSnippetForDownload(snippetId, "other", "token")
        }
    }

    @Test
    fun `getSnippetForDownload should return asset key when owner`() {
        val snippetId = UUID.randomUUID()
        val metadata = SnippetMetadata(snippetId, "name", "ps", "1.0", "desc", "owner")
        val version = SnippetVersion(UUID.randomUUID(), "key", Date(), "1.0", metadata)

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))
        Mockito.`when`(
            snippetVersionRepository.findLatestBySnippetId(any(UUID::class.java), any(Pageable::class.java)),
        ).thenReturn(PageImpl(listOf(version)))

        val result = snippetService.getSnippetForDownload(snippetId, "owner", "token")
        assertEquals("key", result)
    }

    @Test
    fun `getSnippetForDownload should return asset key when shared`() {
        val snippetId = UUID.randomUUID()
        val metadata = SnippetMetadata(snippetId, "name", "ps", "1.0", "desc", "owner")
        val version = SnippetVersion(UUID.randomUUID(), "key", Date(), "1.0", metadata)

        Mockito.`when`(snippetRepository.findById(snippetId)).thenReturn(Optional.of(metadata))
        Mockito.`when`(permissionService.hasReadPermission(snippetId, "token")).thenReturn(true)
        Mockito.`when`(
            snippetVersionRepository.findLatestBySnippetId(any(UUID::class.java), any(Pageable::class.java)),
        ).thenReturn(PageImpl(listOf(version)))

        val result = snippetService.getSnippetForDownload(snippetId, "other", "token")
        assertEquals("key", result)
    }
}
