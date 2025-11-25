package ingsist.snippet.domain

import ingsist.snippet.dtos.ExecuteResDTO

fun processEngineResult(result: ExecuteResDTO): ValidationResult {
    if (result.errors.isNotEmpty()) {
        return ValidationResult.Invalid(
            result.errors,
        )
    }
    // if error list is empty -> valid
    return ValidationResult.Valid(
        message = "Snippet passed validation successfully.",
    )
}
