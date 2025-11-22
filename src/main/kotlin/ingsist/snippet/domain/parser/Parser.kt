package ingsist.snippet.domain.parser

interface Parser {
    fun validate(code: String): ValidationResult
}