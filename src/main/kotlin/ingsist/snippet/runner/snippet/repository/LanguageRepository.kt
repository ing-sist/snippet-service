package ingsist.snippet.runner.snippet.repository

import ingsist.snippet.runner.snippet.domain.LanguageConfig
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LanguageRepository : JpaRepository<LanguageConfig, UUID> {
    fun findByLanguage(language: String): LanguageConfig?
}
