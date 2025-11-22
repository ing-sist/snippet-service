package ingsist.snippet.repository

import ingsist.snippet.domain.SnippetEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository



@Repository

interface SnippetRepository : JpaRepository<SnippetEntity, String> {

    suspend fun findByName(name: String): SnippetEntity?
}
