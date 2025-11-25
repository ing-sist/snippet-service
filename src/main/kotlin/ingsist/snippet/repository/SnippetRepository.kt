package ingsist.snippet.repository

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
