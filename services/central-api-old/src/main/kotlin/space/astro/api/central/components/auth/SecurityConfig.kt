package space.astro.api.central.components.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import space.astro.api.central.components.AuthFilter

@Configuration
class SecurityConfig(
    private val authFilter: AuthFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .addFilter(authFilter)
            .authorizeHttpRequests {
                it.anyRequest().authenticated()
            }
            .build()
    }
}