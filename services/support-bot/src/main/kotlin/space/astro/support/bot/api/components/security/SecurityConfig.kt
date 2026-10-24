package space.astro.support.bot.api.components.security

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val authFilter: AuthFilter,
) {
    // disable automatic registration of filters that are declared in the chain
    // they would not run twice, but it's cleaner to register them only once
    @Bean
    fun authFilterRegistration(filter: AuthFilter) = FilterRegistrationBean(filter).apply { isEnabled = false }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .requestCache { it.disable() }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ -> response.status = HttpStatus.UNAUTHORIZED.value() }
                it.accessDeniedHandler { _, response, _ -> response.status = HttpStatus.FORBIDDEN.value() }
            }.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it
                    .requestMatchers("/docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.build()
}
