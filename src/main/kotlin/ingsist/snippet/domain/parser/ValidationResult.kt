package ingsist.snippet.domain.parser

sealed class ValidationResult {
    data class Valid(
        val message: String
    ): ValidationResult()

    data class Invalid(
        val ruleViolated: String,
        val line: Int,
        val column: Int,
        val message: String
    ): ValidationResult()
}