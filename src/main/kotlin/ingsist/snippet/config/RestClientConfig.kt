package ingsist.snippet.config

import ingsist.snippet.auth.CachedTokenService
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {
    companion object {
        private const val CORRELATION_ID_KEY = "correlation-id"
        private const val CORRELATION_ID_HEADER = "X-Correlation-Id"
    }

    @Bean
    fun engineRestClient(
        @Value("\${external.engine.url}") url: String,
        cachedTokenService: CachedTokenService,
    ): RestClient {
        return RestClient.builder()
            .baseUrl(url)
            .requestInterceptor { request, body, execution ->
                MDC.get(CORRELATION_ID_KEY)?.let { corrId ->
                    request.headers.add(CORRELATION_ID_HEADER, corrId)
                }

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
            .requestInterceptor { request, body, execution ->
                MDC.get(CORRELATION_ID_KEY)?.let { corrId ->
                    request.headers.add(CORRELATION_ID_HEADER, corrId)
                }
                execution.execute(request, body)
            }
            .baseUrl(url)
            .build()
    }
}
