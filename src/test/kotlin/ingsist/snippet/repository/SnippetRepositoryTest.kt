package ingsist.snippet.repository

import ingsist.snippet.test.snippetMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SnippetRepository")
class SnippetRepositoryTest {
    @Autowired
    private lateinit var snippetRepository: SnippetRepository

    @Test
    @DisplayName("GIVEN own and shared snippets WHEN finding all by owner or IDs THEN returns both")
    fun `findAllByOwnerIdOrIdIn returns own and shared snippets`() {
        val ownerId = "owner"
        val otherUser = "other"

        val ownSnippet =
            snippetMetadata {
                this.ownerId = ownerId
                name = "Own Snippet"
            }

        val sharedSnippet =
            snippetMetadata {
                this.ownerId = otherUser
                name = "Shared Snippet"
            }

        val otherSnippet =
            snippetMetadata {
                this.ownerId = otherUser
                name = "Other Snippet"
            }

        snippetRepository.save(ownSnippet)
        snippetRepository.save(sharedSnippet)
        snippetRepository.save(otherSnippet)

        val sharedIds = listOf(sharedSnippet.id)
        val pageable = PageRequest.of(0, 10)

        val result = snippetRepository.findAllByOwnerIdOrIdIn(ownerId, sharedIds, pageable)

        assertEquals(2, result.totalElements)
        assertTrue(result.content.any { it.id == ownSnippet.id })
        assertTrue(result.content.any { it.id == sharedSnippet.id })
        assertTrue(result.content.none { it.id == otherSnippet.id })
    }
}
