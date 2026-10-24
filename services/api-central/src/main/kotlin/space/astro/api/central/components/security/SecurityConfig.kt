package space.astro.api.central.components.security

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.api.central.components.security.ratelimit.IpRateLimitFilter
import space.astro.shared.core.properties.api_central.CentralApiProperties
import space.astro.shared.core.utils.api.CentralApiEndpoint

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val authFilter: AuthFilter,
    private val ipRateLimitFilter: IpRateLimitFilter,
    private val exceptionHandler: ControllersExceptionHandler,
    private val centralApiProperties: CentralApiProperties,
) {
    private companion object {
        /** Username to configure in the basic auth of the Chargebee webhooks */
        const val CHARGEBEE_WEBHOOK_USERNAME = "chargebee"
    }

    // disable automatic registration of filters that are declared in the chain
    // they would not run twice, but it's cleaner to register them only once
    @Bean
    fun authFilterRegistration(filter: AuthFilter) = FilterRegistrationBean(filter).apply { isEnabled = false }

    @Bean
    fun ipRateLimitFilterRegistration(filter: IpRateLimitFilter) = FilterRegistrationBean(filter).apply { isEnabled = false }

    /**
     * Chargebee webhooks, authenticated with basic auth instead of the user session
     */
    @Bean
    @Order(1)
    fun chargebeeWebhooksSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher(
                CentralApiEndpoint.CHARGEBEE_EVENT_SUBSCRIPTION_CREATE,
                CentralApiEndpoint.CHARGEBEE_EVENT_SUBSCRIPTION_CANCEL,
            ).sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }
            .httpBasic {}
            .userDetailsService(
                InMemoryUserDetailsManager(
                    User
                        .withUsername(CHARGEBEE_WEBHOOK_USERNAME)
                        .password("{noop}${centralApiProperties.chargebeeWebhookToken}")
                        .roles("CHARGEBEE")
                        .build(),
                ),
            ).authorizeHttpRequests { it.anyRequest().authenticated() }
            .build()

    @Bean
    @Order(2)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .cors {}
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .requestCache { it.disable() }
            .csrf { it.disable() }
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
            }.addFilterBefore(ipRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(authFilter, IpRateLimitFilter::class.java)
//            .addFilterAfter(userIdRateLimitFilter, AuthFilter::class.java)
            .authorizeHttpRequests {
                it
                    .requestMatchers("/docs/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .permitAll() // served on the management port, not exposed by the ingress
                    .requestMatchers(
                        CentralApiEndpoint.DISCORD_LOGIN,
                        CentralApiEndpoint.DISCORD_OAUTH_CALLBACK,
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
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
