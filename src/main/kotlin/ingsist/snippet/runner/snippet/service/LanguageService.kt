package ingsist.snippet.runner.snippet.service

import ingsist.snippet.engine.EngineService
import ingsist.snippet.runner.snippet.domain.LanguageConfig
import ingsist.snippet.runner.snippet.dtos.SupportedLanguageDto
import ingsist.snippet.runner.snippet.repository.LanguageRepository
import ingsist.snippet.shared.exception.ExternalServiceException
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException

@Service
class LanguageService(
    private val languageRepository: LanguageRepository,
    private val engineService: EngineService,
) {
    @PostConstruct
    fun syncLanguages() {
        try {
            val languages = engineService.getLanguages()
            updateLanguages(languages)
        } catch (e: ExternalServiceException) {
            println("Failed to sync languages from Engine: ${e.message}")
        } catch (e: RestClientException) {
            println("Failed to sync languages from Engine: ${e.message}")
        }
    }

    @Transactional
    fun updateLanguages(languages: List<SupportedLanguageDto>) {
        // Group by language name to handle multiple versions
        val groupedLanguages = languages.groupBy { it.name }

        groupedLanguages.forEach { (name, dtos) ->
            val versions = dtos.map { it.version }
            val extension = dtos.first().extension
            val existingConfig = languageRepository.findByLanguage(name)

            if (existingConfig != null) {
                existingConfig.versions = versions
                existingConfig.extension = extension
                languageRepository.save(existingConfig)
            } else {
                languageRepository.save(
                    LanguageConfig(
                        language = name,
                        versions = versions,
                        extension = extension,
                    ),
                )
            }
        }
    }

    fun getSupportedLanguages(): List<LanguageConfig> {
        return languageRepository.findAll()
    }

    fun getExtension(language: String): String {
        return languageRepository.findByLanguage(language)?.extension ?: "txt"
    }

    fun isLanguageSupported(
        language: String,
        version: String,
    ): Boolean {
        val config = languageRepository.findByLanguage(language) ?: return false
        return config.versions.contains(version)
    }
}
