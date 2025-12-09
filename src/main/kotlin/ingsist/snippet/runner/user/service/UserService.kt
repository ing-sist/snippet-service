package ingsist.snippet.runner.user.service

import ingsist.snippet.auth.service.AuthService
import ingsist.snippet.runner.snippet.domain.FormattingConfig
import ingsist.snippet.runner.snippet.domain.LintingConfig
import ingsist.snippet.runner.snippet.domain.OwnerConfig
import ingsist.snippet.runner.snippet.dtos.FormattingRulesDTO
import ingsist.snippet.runner.snippet.dtos.LintingRulesDTO
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
        val allUsers = authService.getUsers("", token)
        return allUsers.filter { it.email.contains(email, ignoreCase = true) }
    }

    fun updateUserConfig(
        ownerId: String,
        dto: OwnerConfigDto,
    ): OwnerConfig {
        val lintingConfig =
            LintingConfig(
                identifierNamingType = dto.linting.identifierNamingType,
                printlnSimpleArg = dto.linting.printlnSimpleArg,
                readInputSimpleArg = dto.linting.readInputSimpleArg,
            )
        val formattingConfig =
            FormattingConfig(
                indentation = dto.formatting.indentation,
                spaceBeforeColon = dto.formatting.spaceBeforeColon,
                spaceAfterColon = dto.formatting.spaceAfterColon,
                spaceAroundAssignment = dto.formatting.spaceAroundAssignment,
                spaceAroundOperators = dto.formatting.spaceAroundOperators,
                maxSpaceBetweenTokens = dto.formatting.maxSpaceBetweenTokens,
                lineBreakBeforePrintln = dto.formatting.lineBreakBeforePrintln,
                lineBreakAfterSemiColon = dto.formatting.lineBreakAfterSemiColon,
                inlineBraceIfStatement = dto.formatting.inlineBraceIfStatement,
                belowLineBraceIfStatement = dto.formatting.belowLineBraceIfStatement,
                braceLineBreak = dto.formatting.braceLineBreak,
                keywordSpacingAfter = dto.formatting.keywordSpacingAfter,
            )

        val existingConfig = userRepository.findByIdOrNull(ownerId)
        val configToSave =
            existingConfig?.copy(
                linting = lintingConfig,
                formatting = formattingConfig,
            ) ?: OwnerConfig(
                ownerId = ownerId,
                linting = lintingConfig,
                formatting = formattingConfig,
            )
        return userRepository.save(configToSave)
    }

    open fun getUserConfig(ownerId: String): OwnerConfigDto {
        val configEntity: OwnerConfig? = userRepository.findByIdOrNull(ownerId)

        return configEntity?.toDto()
            ?: createDefaultConfig()
    }

    private fun OwnerConfig.toDto(): OwnerConfigDto =
        OwnerConfigDto(
            linting =
                LintingRulesDTO(
                    identifierNamingType = this.linting.identifierNamingType,
                    printlnSimpleArg = this.linting.printlnSimpleArg,
                    readInputSimpleArg = this.linting.readInputSimpleArg,
                ),
            formatting =
                FormattingRulesDTO(
                    indentation = this.formatting.indentation,
                    spaceBeforeColon = this.formatting.spaceBeforeColon,
                    spaceAfterColon = this.formatting.spaceAfterColon,
                    spaceAroundAssignment = this.formatting.spaceAroundAssignment,
                    spaceAroundOperators = this.formatting.spaceAroundOperators,
                    maxSpaceBetweenTokens = this.formatting.maxSpaceBetweenTokens,
                    lineBreakBeforePrintln = this.formatting.lineBreakBeforePrintln,
                    lineBreakAfterSemiColon = this.formatting.lineBreakAfterSemiColon,
                    inlineBraceIfStatement = this.formatting.inlineBraceIfStatement,
                    belowLineBraceIfStatement = this.formatting.belowLineBraceIfStatement,
                    braceLineBreak = this.formatting.braceLineBreak,
                    keywordSpacingAfter = this.formatting.keywordSpacingAfter,
                ),
        )

    private fun createDefaultConfig(): OwnerConfigDto {
        return OwnerConfigDto(
            linting =
                LintingRulesDTO(
                    identifierNamingType = "camel",
                    printlnSimpleArg = false,
                    readInputSimpleArg = false,
                ),
            formatting =
                FormattingRulesDTO(
                    indentation = 2,
                    spaceBeforeColon = false,
                    spaceAfterColon = true,
                    spaceAroundAssignment = true,
                    spaceAroundOperators = true,
                    maxSpaceBetweenTokens = false,
                    lineBreakBeforePrintln = 0,
                    lineBreakAfterSemiColon = true,
                    inlineBraceIfStatement = false,
                    belowLineBraceIfStatement = false,
                    braceLineBreak = 1,
                    keywordSpacingAfter = true,
                ),
        )
    }
}
