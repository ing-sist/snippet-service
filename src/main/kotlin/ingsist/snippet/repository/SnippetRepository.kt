package ingsist.snippet.repository

<<<<<<< HEAD
import ingsist.snippet.domain.SnippetMetadata
import ingsist.snippet.domain.SnippetVersion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SnippetRepository : JpaRepository<SnippetMetadata, String> {
    suspend fun findByName(name: String): SnippetMetadata?
}

@Repository
interface SnippetVersionRepository : JpaRepository<SnippetVersion, UUID>
=======
import ingsist.snippet.domain.SnippetEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
class SnippetRepository(
    private val repository: JpaRepository<SnippetEntity, String>
) {

    fun saveSnippet(snippet: SnippetEntity): SnippetEntity {
        return repository.save(snippet)
    }
}
>>>>>>> 2d4a23d (feat/useCase#1: created snippet controller, service repo & connected to ps parser (not finished yet))
