package ingsist.snippet.engine

import ingsist.snippet.dtos.SnippetParserRequestDTO
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class EngineClient(private val baseUrl: String) {
    private val webClient = WebClient.create(baseUrl)

    fun parse(snippet: SnippetParserRequestDTO) {
        webClient.post()
            .uri("/v1/engine/parse")
            .bodyValue(snippet)
            )
    }
}