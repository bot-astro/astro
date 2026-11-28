package space.astro.api.central.components.security

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.api.central.components.IdempotencyFilter
import space.astro.api.central.components.RequestIdFilter
import space.astro.api.central.components.security.ratelimit.IpRateLimitFilter
import space.astro.api.central.components.security.ratelimit.UserIdRateLimitFilter
import space.astro.api.central.models.auth.AuthScope
import space.astro.shared.core.exceptions.AInsufficientScopeException
import space.astro.shared.core.properties.api_central.CentralApiProperties
import space.astro.shared.core.utils.api.CentralApiEndpoint

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val authFilter: AuthFilter,
    private val ipRateLimitFilter: IpRateLimitFilter,
    private val userIdRateLimitFilter: UserIdRateLimitFilter,
    private val idempotencyFilter: IdempotencyFilter,
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

    @Bean
    fun userIdRateLimitFilterRegistration(filter: UserIdRateLimitFilter) = FilterRegistrationBean(filter).apply { isEnabled = false }

    @Bean
    fun idempotencyFilterRegistration(filter: IdempotencyFilter) = FilterRegistrationBean(filter).apply { isEnabled = false }

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

    /**
     * Vote Tracker forwarding, the signature is verified by the controller because it needs the raw body
     */
    @Bean
    @Order(2)
    fun voteTrackerWebhookSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher(CentralApiEndpoint.VOTE_TRACKER_EVENTS)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()

    @Bean
    @Order(3)
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
                // the only rule that denies a logged in user is the scope check at the end of the chain
                it.accessDeniedHandler { request, response, _ ->
                    val requiredScope = if (request.method.isReadMethod()) AuthScope.GUILDS_READ else AuthScope.GUILDS_WRITE
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\", scope=\"$requiredScope\"")
                    exceptionHandler.writeException(response, AInsufficientScopeException(requiredScope))
                }
            }
            // MCP requests all come from the IP of the MCP server, so logged in users are limited by user id
            // and only anonymous requests by IP
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(ipRateLimitFilter, AuthFilter::class.java)
            .addFilterAfter(userIdRateLimitFilter, IpRateLimitFilter::class.java)
            .addFilterAfter(idempotencyFilter, UserIdRateLimitFilter::class.java)
            .authorizeHttpRequests {
                it
                    .requestMatchers("/docs/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .permitAll() // served on the management port, not exposed by the ingress
                    .requestMatchers(
                        CentralApiEndpoint.DISCORD_LOGIN,
                        CentralApiEndpoint.DISCORD_OAUTH_CALLBACK,
                        CentralApiEndpoint.OAUTH_SERVER_METADATA,
                        CentralApiEndpoint.OAUTH_AUTHORIZE,
                        CentralApiEndpoint.OAUTH_CONSENT,
                        CentralApiEndpoint.OAUTH_TOKEN,
                        CentralApiEndpoint.OAUTH_INTROSPECT,
                        CentralApiEndpoint.OAUTH_REVOKE,
                    ).permitAll()
                    .requestMatchers(RequestMatcher { it.method.isReadMethod() })
                    .hasAuthority(AuthScope.GUILDS_READ)
                    .anyRequest()
                    .hasAuthority(AuthScope.GUILDS_WRITE)
            }.build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                allowedOrigins = centralApiProperties.corsAllowedOrigins
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("authorization", "content-type", "idempotency-key")
                exposedHeaders =
                    listOf(
                        "Retry-After",
                        RequestIdFilter.HEADER,
                        IdempotencyFilter.REPLAYED_HEADER,
                        "X-Rate-Limit-Limit",
                        "X-Rate-Limit-Remaining",
                    )
                allowCredentials = true
            }

        // browser based MCP clients on any origin read the metadata and use the token endpoint, with no cookies
        val publicOAuthConfig =
            CorsConfiguration().apply {
                allowedOriginPatterns = listOf("*")
                allowedMethods = listOf("GET", "POST", "OPTIONS")
                allowedHeaders = listOf("content-type")
                allowCredentials = false
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(CentralApiEndpoint.OAUTH_SERVER_METADATA, publicOAuthConfig)
            registerCorsConfiguration(CentralApiEndpoint.OAUTH_TOKEN, publicOAuthConfig)
            registerCorsConfiguration(CentralApiEndpoint.OAUTH_REVOKE, publicOAuthConfig)
            registerCorsConfiguration("/**", config)
        }
    }

    private fun String.isReadMethod() = this == "GET" || this == "HEAD"
}
