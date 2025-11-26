package ingsist.snippet.domain.asset

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class AssetClient(private val baseUrl: String) {
    private val webClient = WebClient.create(baseUrl)

    fun upload(container: String, key: String, content: String) {
        webClient.put()
            .uri("/v1/asset/{container}/{key}", container, key)
            .bodyValue(content)
    }
}