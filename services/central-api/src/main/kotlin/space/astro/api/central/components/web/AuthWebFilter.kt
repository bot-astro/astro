package space.astro.api.central.components.web

import com.fasterxml.jackson.databind.ObjectMapper
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
import space.astro.api.central.configs.Mappings
import space.astro.api.central.configs.ExchangeAttributeNames
import space.astro.api.central.models.discord.OAuth2AuthorizationResponseDto
import space.astro.api.central.services.discord.DiscordUserTokenFetchService
import space.astro.api.central.services.discord.DiscordUserTokenPersistenceService
import space.astro.api.central.services.dashboard.WebSessionService
import space.astro.api.central.util.SessionCookieUtil
import space.astro.shared.core.components.io.DataSerializer
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
    private val dataSerializer: DataSerializer
): WebFilter {
    private val base64Decoder = Base64.getDecoder()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response
        val requestPath = request.path.toString()

        val sessionCookie = request.cookies.getFirst(centralApiConfig.sessionCookieName)
        println("got cookie $sessionCookie")
        val sessionObjectAsString = sessionCookie?.let { SessionCookieUtil.unseal(it.value, centralApiConfig.sessionCookiePassword) }
        println("got session $sessionObjectAsString")
        val sessionToken = sessionObjectAsString?.let { dataSerializer.deserialize<OAuth2AuthorizationResponseDto>(sessionObjectAsString).token }
        println("got token $sessionToken")

        response.headers.set("Access-Control-Allow-Origin", "http://localhost:3000")
        response.headers.set("Access-Control-Allow-Methods", "*")
        response.headers.set("Access-Control-Allow-Headers", "*")

        // chain preflight requests
        if (request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }

        //////////////
        /// STATUS ///
        //////////////
        if (requestPath.startsWith(Mappings.Status.STATUS)) {
            return chain.filter(exchange)
        }


        ////////////////
        /// API DOCS ///
        ////////////////
        if (requestPath.startsWith(Mappings.Docs.API_DOCS)
            || requestPath.startsWith(Mappings.Docs.WEBJARS)
            || requestPath.startsWith(Mappings.Docs.SWAGGER))
        {
            return chain.filter(exchange)
        }


        /////////////
        /// LOGIN ///
        /////////////
        if (requestPath.startsWith(Mappings.Dashboard.Prefixes.LOGIN)) {
            return chain.filter(exchange)
        }


        ////////////////////////
        /// CHARGEBEE EVENTS ///
        ////////////////////////
        if (requestPath.startsWith(Mappings.Chargebee.Prefixes.EVENT)) {
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

        if (requestPath.startsWith(Mappings.Dashboard.Prefixes.DASHBOARD)
            || requestPath.startsWith(Mappings.Chargebee.PORTAL_SESSION))
        {
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