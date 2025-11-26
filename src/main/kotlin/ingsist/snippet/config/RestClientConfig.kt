package ingsist.snippet.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig(
    @Value("\${service.asset.url}") private val assetUrl: String,
    @Value("\${service.engine.url}") private val engineUrl: String,
    @Value("\${service.auth.url}") private val authUrl: String,
) {
    @Bean
    fun assetRestClient(
        @Value("\${external.asset.url}") url: String,
    ): RestClient {
        return RestClient.builder()
            .baseUrl(assetUrl)
            .build()
    }

    @Bean
    fun engineRestClient(
        @Value("\${external.engine.url}") url: String,
    ): RestClient {
        return RestClient.builder()
            .baseUrl(engineUrl)
            .build()
    }

    @Bean
    fun authRestClient(
        @Value("\${external.auth.url}") url: String,
    ): RestClient {
        return RestClient.builder()
            .baseUrl(authUrl)
            .build()
    }
}
