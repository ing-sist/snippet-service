package ingsist.snippet.runner.domain

import ingsist.snippet.runner.dtos.ValidateResDto

fun processEngineResult(result: ValidateResDto): ValidationResult {
    if (result.error.isNotEmpty()) {
        return ValidationResult.Invalid(
            result.error,
        )
    }
    // if error list is empty -> valid
    return ValidationResult.Valid(
        message = "Snippet passed validation successfully.",
    )
}
