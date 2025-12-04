package ingsist.snippet.runner.service

import UserRepository
import ingsist.snippet.runner.domain.OwnerConfig
import ingsist.snippet.runner.dtos.OwnerConfigDTO
import ingsist.snippet.runner.repository.SnippetRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val snippetRepository: SnippetRepository,
    private val userRepository: UserRepository,
) {
    fun updateUserConfig(
        ownerId: String,
        dto: OwnerConfigDTO,
    ): OwnerConfig {
        val newConfig =
            OwnerConfig(
                ownerId = ownerId,
                noExpressionsInPrintLine = dto.noExpressionsInPrintLine,
                noUnusedVars = dto.noUnusedVars,
                noUndefVars = dto.noUndefVars,
                noUnusedParams = dto.noUnusedParams,
                indentation = dto.indentation,
                openIfBlockOnSameLine = dto.openIfBlockOnSameLine,
                maxLineLength = dto.maxLineLength,
                noTrailingSpaces = dto.noTrailingSpaces,
                noMultipleEmptyLines = dto.noMultipleEmptyLines,
            )
        return userRepository.save(newConfig)
    }

    fun getUserConfig(snippetId: UUID): OwnerConfig {
        val snippet = snippetRepository.findById(snippetId)
        val ownerId = snippet.get().ownerId
        return userRepository.findByIdOrNull(ownerId) ?: createDefaultConfig(ownerId)
    }

    private fun createDefaultConfig(ownerId: String): OwnerConfig {
        return OwnerConfig(
            ownerId = ownerId,
            noExpressionsInPrintLine = false,
            noUnusedVars = true,
            noUndefVars = true,
            noUnusedParams = true,
            indentation = 4,
            openIfBlockOnSameLine = true,
            maxLineLength = 80,
            noTrailingSpaces = true,
            noMultipleEmptyLines = true,
        )
    }
}
