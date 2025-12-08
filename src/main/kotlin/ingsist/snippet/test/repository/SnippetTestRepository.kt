package ingsist.snippet.test.repository

import ingsist.snippet.test.model.entity.SnippetTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SnippetTestRepository : JpaRepository<SnippetTest, UUID> {
    fun findAllBySnippetId(snippetId: UUID): List<SnippetTest>

    fun findBySnippetIdAndTestId(
        snippetId: UUID,
        testId: UUID,
    ): SnippetTest?
}
