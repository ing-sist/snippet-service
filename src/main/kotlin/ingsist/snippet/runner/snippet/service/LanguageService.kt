package ingsist.snippet.runner.snippet.service

import ingsist.snippet.engine.EngineServiceInterface
import ingsist.snippet.runner.snippet.domain.LanguageConfig
import ingsist.snippet.runner.snippet.dtos.SupportedLanguageDto
import ingsist.snippet.runner.snippet.repository.LanguageRepository
import ingsist.snippet.shared.exception.ExternalServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException

@Service
class LanguageService(
    private val languageRepository: LanguageRepository,
    private val engineService: EngineServiceInterface,
) {
    fun getSupportedLanguages(): List<LanguageConfig> {
        try {
            val languages = engineService.getLanguages()
            updateLanguages(languages)
        } catch (e: ExternalServiceException) {
            println("Warning: Could not sync languages from Engine. Using cached data. Error: ${e.message}")
        } catch (e: RestClientException) {
            println("Warning: Could not sync languages from Engine. Using cached data. Error: ${e.message}")
        }
        return languageRepository.findAll()
    }

    @Transactional
    private fun updateLanguages(languages: List<SupportedLanguageDto>) {
        languages.forEach { dto ->
            val existingConfig = languageRepository.findByLanguage(dto.name)

            if (existingConfig != null) {
                if (existingConfig.versions != dto.version || existingConfig.extension != dto.extension) {
                    existingConfig.versions = dto.version
                    existingConfig.extension = dto.extension
                    languageRepository.save(existingConfig)
                }
            } else {
                languageRepository.save(
                    LanguageConfig(
                        language = dto.name,
                        versions = dto.version,
                        extension = dto.extension,
                    ),
                )
            }
        }
    }

    fun getExtension(language: String): String {
        val config = languageRepository.findAll().find { it.language.equals(language, ignoreCase = true) }
        return config?.extension ?: "txt"
    }

    fun isLanguageSupported(
        language: String,
        version: String,
    ): Boolean {
        val configs = getSupportedLanguages()
        val config = configs.find { it.language.equals(language, ignoreCase = true) } ?: return false
        return config.versions.contains(version)
    }
}
