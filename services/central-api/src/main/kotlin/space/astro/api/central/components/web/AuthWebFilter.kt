package space.astro.api.central.components.web

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import space.astro.api.central.configs.CentralApiConfig
import space.astro.shared.core.components.web.CentralApiRoutes
import space.astro.api.central.services.discord.DiscordUserTokenFetchService
import space.astro.api.central.services.discord.DiscordUserTokenPersistenceService
import space.astro.api.central.services.dashboard.WebSessionService
import space.astro.api.central.util.ExchangeAttributeNames
import space.astro.shared.core.configs.ChargebeeConfig
import space.astro.shared.core.configs.KubeConfig
import java.util.Base64

@Component
class AuthWebFilter(
    private val webSessionService: WebSessionService,
    private val chargebeeConfig: ChargebeeConfig,
    private val centralApiConfig: CentralApiConfig,
    private val kubeConfig: KubeConfig,
    private val userTokenPersistenceService: DiscordUserTokenPersistenceService,
    private val userTokenFetchService: DiscordUserTokenFetchService,
): WebFilter {
    private val base64Decoder = Base64.getDecoder()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response
        val requestPath = request.path.toString()

        response.headers.set("Access-Control-Allow-Credentials", "true")
        response.headers.set("Access-Control-Allow-Origin", centralApiConfig.sessionCookieAllowOrigin)
        response.headers.set("Access-Control-Allow-Methods", centralApiConfig.corsAllowedMethods)
        response.headers.set("Access-Control-Allow-Headers", centralApiConfig.corsAllowedHeaders)

        // chain preflight requests
        if (request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }

        //////////////
        /// STATUS ///
        //////////////
        if (requestPath.startsWith(CentralApiRoutes.Status.STATUS)) {
            return chain.filter(exchange)
        }


        ////////////////
        /// API DOCS ///
        ////////////////
        if (requestPath.startsWith(CentralApiRoutes.Docs.API_DOCS)
            || requestPath.startsWith(CentralApiRoutes.Docs.WEBJARS)
            || requestPath.startsWith(CentralApiRoutes.Docs.SWAGGER))
        {
            return chain.filter(exchange)
        }


        /////////////
        /// LOGIN ///
        /////////////
        if (requestPath.startsWith(CentralApiRoutes.Dashboard.Prefixes.LOGIN)) {
            return chain.filter(exchange)
        }


        ////////////////////////
        /// CHARGEBEE EVENTS ///
        ////////////////////////
        if (requestPath.startsWith(CentralApiRoutes.Chargebee.Prefixes.EVENT)) {
            return mono {
                val webhookTokenEncoded = request.headers["Authorization"]?.get(0)?.removePrefix("Basic ")

                if (webhookTokenEncoded == null) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                val webhookToken = String(base64Decoder.decode(webhookTokenEncoded)).split(":").lastOrNull()
                if (webhookToken != chargebeeConfig.webhookToken) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                chain.filter(exchange).awaitSingleOrNull()
            }
        }

        ////////////////////////////////////////////
        /// DASHBOARD & CHARGEBEE PORTAL SESSION ///
        ////////////////////////////////////////////

        if (requestPath.startsWith(CentralApiRoutes.Dashboard.Prefixes.DASHBOARD)
            || requestPath.startsWith(CentralApiRoutes.Chargebee.PORTAL_SESSION)
            || requestPath.startsWith(CentralApiRoutes.Chargebee.CHECKOUT)
            || requestPath.startsWith(CentralApiRoutes.Chargebee.USER_ACTIVE_SUBSCRIPTIONS)
            || requestPath.startsWith(CentralApiRoutes.Chargebee.LOGGED_USER_ACTIVE_SUBSCRIPTIONS))
        {
            val sessionToken = request.cookies.getFirst(centralApiConfig.sessionCookieName)?.value
                ?: request.headers["Authorization"]?.get(0)?.removePrefix("Bearer ")

            return mono {
                if (sessionToken == null) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                val userID = webSessionService.getUserIdFromSession(sessionToken)

                if (userID == null) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                val (tokenPayload, isNotExpired) = userTokenPersistenceService.getToken(userID) ?: run {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                val accessToken = if (isNotExpired) {
                    tokenPayload.accessToken
                } else {
                    val newAuthorizationWrapperDto = userTokenFetchService.refreshToken(tokenPayload.refreshToken)
                    newAuthorizationWrapperDto.token.accessToken
                }

                exchange.attributes[ExchangeAttributeNames.USER_ID] = userID
                exchange.attributes[ExchangeAttributeNames.ACCESS_TOKEN] = accessToken

                chain.filter(exchange).awaitSingleOrNull()
            }
        }

        /////////////////////
        /// ALL REMAINING ///
        /////////////////////
        return mono {
            val authHeader = request.headers["Authorization"]?.get(0)
                ?: run {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }
            if (authHeader != centralApiConfig.auth && authHeader != kubeConfig.lifecycleAuthorization) {
                response.statusCode = HttpStatus.UNAUTHORIZED
                return@mono null
            }

            chain.filter(exchange).awaitSingleOrNull()
        }
    }
}
