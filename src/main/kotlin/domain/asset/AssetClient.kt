package domain.asset

import org.springframework.web.reactive.function.client.WebClient

class AssetClient(baseUrl: String) {
    private val webClient = WebClient.create(baseUrl)

    fun upload(container: String, key: String, content: String) {
        webClient.put()
            .uri("/v1/asset/{container}/{key}", container, key)
            .bodyValue(content)
    }
}