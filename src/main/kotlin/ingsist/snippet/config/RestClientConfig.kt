package ingsist.snippet.config

import ingsist.snippet.auth.CachedTokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {
    @Bean
    fun engineRestClient(
        @Value("\${external.engine.url}") url: String,
        cachedTokenService: CachedTokenService,
    ): RestClient {
        return RestClient.builder()
            .baseUrl(url)
            .requestInterceptor { request, body, execution ->
                // 2. Obtenemos el token M2M válido (cacheado o nuevo)
                val token = cachedTokenService.getToken()

                // 3. Lo pegamos en la frente de la petición
                request.headers.add(HttpHeaders.AUTHORIZATION, "Bearer $token")

                execution.execute(request, body)
            }
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
