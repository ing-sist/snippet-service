package ingsist.snippet.runner.user.service

import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.runner.snippet.domain.OwnerConfig
import ingsist.snippet.runner.snippet.dtos.OwnerConfigDto
import ingsist.snippet.runner.user.dtos.UserResponseDTO
import ingsist.snippet.runner.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val authService: AuthService,
) {
    fun searchUsers(
        email: String,
        token: String,
    ): List<UserResponseDTO> {
        return authService.getUsers(email, token)
    }

    fun updateUserConfig(
        ownerId: String,
        dto: OwnerConfigDto,
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

    open fun getUserConfig(ownerId: String): OwnerConfigDto {
        val configEntity: OwnerConfig? = userRepository.findByIdOrNull(ownerId)

        return configEntity?.toDto()
            ?: createDefaultConfig()
    }

    private fun OwnerConfig.toDto(): OwnerConfigDto =
        OwnerConfigDto(
            noExpressionsInPrintLine = this.noExpressionsInPrintLine,
            noUnusedVars = this.noUnusedVars,
            noUndefVars = this.noUndefVars,
            noUnusedParams = this.noUnusedParams,
            indentation = this.indentation,
            openIfBlockOnSameLine = this.openIfBlockOnSameLine,
            maxLineLength = this.maxLineLength,
            noTrailingSpaces = this.noTrailingSpaces,
            noMultipleEmptyLines = this.noMultipleEmptyLines,
        )

    private fun createDefaultConfig(): OwnerConfigDto {
        return OwnerConfigDto(
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
