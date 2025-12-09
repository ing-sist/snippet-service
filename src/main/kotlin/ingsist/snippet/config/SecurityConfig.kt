package ingsist.snippet.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuer: String,
    @Value("\${auth0.audience}") private val audience: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/health/error", "/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                }
            }

        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val jwtDecoder = JwtDecoders.fromIssuerLocation<NimbusJwtDecoder>(issuer)

        val audienceValidator: OAuth2TokenValidator<Jwt> =
            JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
            ) { audiences -> audiences.contains(audience) }

        val withIssuer = JwtValidators.createDefaultWithIssuer(issuer)
        val withAudience = DelegatingOAuth2TokenValidator(withIssuer, audienceValidator)

        jwtDecoder.setJwtValidator(withAudience)
        return jwtDecoder
    }
}
