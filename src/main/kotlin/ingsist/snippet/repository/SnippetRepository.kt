package ingsist.snippet.repository

import ingsist.snippet.domain.SnippetMetadata
import ingsist.snippet.domain.SnippetVersion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SnippetRepository : JpaRepository<SnippetMetadata, UUID> {
    // Busca snippets propios O que estén en la lista de IDs compartidos
    @Query("SELECT s FROM SnippetMetadata s WHERE s.ownerId = :userId OR s.id IN :sharedSnippetIds")
    fun findAllByOwnerIdOrIdIn(
        userId: String,
        sharedSnippetIds: List<UUID>,
        pageable: Pageable,
    ): Page<SnippetMetadata>

    // Sobrecarga para cuando no hay snippets compartidos (lista vacía puede dar error en SQL según el driver)
    fun findAllByOwnerId(
        userId: String,
        pageable: Pageable,
    ): Page<SnippetMetadata>

    suspend fun findByName(name: String): SnippetMetadata?
}

@Repository
interface SnippetVersionRepository : JpaRepository<SnippetVersion, UUID>
