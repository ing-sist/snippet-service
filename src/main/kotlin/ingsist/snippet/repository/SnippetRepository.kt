package ing.sist.snippet.repository

import ing.sist.snippet.model.Snippet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SnippetRepository : JpaRepository<Snippet, Long> {
    fun findByIdIn(ids: List<Long>): List<Snippet>
}
