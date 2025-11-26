package ingsist.snippet.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {
    @Bean
    fun assetRestClient(
        @Value("\${external.asset.url}") url: String,
    ): RestClient {
        return RestClient.builder()
            .baseUrl(url)
            .build()
    }

    @Bean
    fun engineRestClient(
        @Value("\${external.engine.url}") url: String,
    ): RestClient {
        return RestClient.builder()
            .baseUrl(url)
            .build()
    }

    @Bean
    fun authRestClient(
        @Value("\${external.auth.url}") url: String,
    ): RestClient {
        return RestClient.builder()
            .baseUrl(url)
            .build()
    }
}
