package ingsist.snippet.runner.snippet.repository

import ingsist.snippet.runner.snippet.domain.SnippetVersion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SnippetVersionRepository : JpaRepository<SnippetVersion, UUID> {
    @Query(
        """
    SELECT v 
    FROM SnippetVersion v 
    WHERE v.snippet.id = :snippetId
    ORDER BY v.createdDate DESC
    """,
    )
    fun findLatestBySnippetId(
        @Param("snippetId") snippetId: UUID,
        pageable: Pageable,
    ): Page<SnippetVersion>
}
