package domain.parser

interface Parser {
    fun validate(code: String): ValidationResult
}