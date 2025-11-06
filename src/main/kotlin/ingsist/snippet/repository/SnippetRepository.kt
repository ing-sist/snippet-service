package ingsist.snippet.repository

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