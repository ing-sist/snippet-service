package ingsist.snippet.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {
    @Bean
    fun assetRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("http://localhost:8080")
            .build()
    }

    @Bean
    fun engineRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("http://localhost:8081")
            .build()
    }

    @Bean
    fun authRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("http://localhost:8082")
            .build()
    }
}