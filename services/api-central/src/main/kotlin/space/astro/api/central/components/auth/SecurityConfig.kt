package space.astro.api.central.components.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.shared.core.properties.api_central.CentralApiProperties
import space.astro.shared.core.utils.api.CentralApiEndpoint


@Configuration
class SecurityConfig(
    private val authFilter: AuthFilter,
    private val exceptionHandler: ControllersExceptionHandler,
    private val centralApiProperties: CentralApiProperties
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .cors {}
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, exception ->
                    exceptionHandler.writeException(response, exception)
                }
                it.accessDeniedHandler { _, response, exception ->
                    exceptionHandler.writeException(response, exception)
                }
            }
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it
                    .requestMatchers("/docs/**").permitAll()
                    .requestMatchers(
                        CentralApiEndpoint.DISCORD_LOGIN,
                        CentralApiEndpoint.DISCORD_OAUTH_CALLBACK
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = centralApiProperties.corsAllowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("authorization", "content-type")
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}