package ingsist.snippet.runner.domain

sealed class ValidationResult {
    data class Valid(
        val message: String,
    ) : ValidationResult()

    data class Invalid(
        val message: List<String>,
    ) : ValidationResult()
}
