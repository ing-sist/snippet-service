package ingsist.snippet.domain.parser

class ExternalParserAdapter<ExtResult>(
    private val validateFunc: (String) -> ExtResult,
    private val mapper: (ExtResult) -> ValidationResult
) : Parser {
    override fun validate(code: String): ValidationResult {
        val ext = validateFunc(code)
        return mapper(ext)
    }
}