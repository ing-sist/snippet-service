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

    fun getCode(container: String, key: String): String? {
        val response = webClient.get()
            .uri("/v1/asset/{container}/{key}", container, key)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        return response
    }

    fun updateCode(container: String, key: String, content: String) {
        webClient.post()
            .uri("/v1/asset/{container}/{key}", container, key)
            .bodyValue(content)
    }
}