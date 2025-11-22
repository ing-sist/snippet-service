package ingsist.snippet.domain.parser

import org.springframework.stereotype.Component

@Component
class ParserRegistry {
    private val parsers: MutableMap<Pair<String, String>, Parser> = mutableMapOf()

    fun registerParser(language: String, version: String, parser: Parser) {
        parsers.put(Pair(language,version), parser)
    }

    fun getParser(language: String, version: String): Parser? {
        return parsers[Pair(language, version)]
    }
}