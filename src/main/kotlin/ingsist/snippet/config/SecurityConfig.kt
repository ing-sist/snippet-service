package ing.sist.snippet.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Deshabilitar CSRF para APIs REST
            .authorizeHttpRequests { requests ->
                requests
                    // Permitir acceso a Swagger/OpenAPI y Actuator
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                    // Todas las demás rutas deben estar autenticadas
                    .anyRequest().authenticated()
            }
            // Configurar como un "Resource Server" de OAuth2
            .oauth2ResourceServer { it.jwt() }
            // No crear sesiones, cada petición debe traer el token JWT
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
        return http.build()
    }
}
