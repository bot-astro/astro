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
import space.astro.api.central.services.WebSessionService
import space.astro.shared.core.configs.ChargebeeConfig
import java.util.Base64

@Component
class CustomWebFilter(
    private val webSessionService: WebSessionService,
    private val chargebeeConfig: ChargebeeConfig,
    private val centralApiConfig: CentralApiConfig
): WebFilter {
    private val base64Decoder = Base64.getDecoder()

    // This web filter is awful I know, but it's temporary other priorities
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response
        val requestPath = request.path.toString()

        response.headers.set("Access-Control-Allow-Origin", "*")
        response.headers.set("Access-Control-Allow-Methods", "*")
        response.headers.set("Access-Control-Allow-Headers", "*")

        // chain preflight requests
        if (request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }

        // chain callback requests
        if (requestPath.startsWith("/auth/id")) {
            return chain.filter(exchange)
        }

        if (requestPath == "/chargebee/cancel") {
            return mono {
                val webhookTokenEncoded = request.headers["Authorization"]?.get(0)?.removePrefix("Basic ")

                if (webhookTokenEncoded == null) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                val webhookToken = String(base64Decoder.decode(webhookTokenEncoded))
                if (webhookToken != chargebeeConfig.webhookToken) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                chain.filter(exchange).awaitSingleOrNull()
            }
        }

        if (requestPath.startsWith("/auth/user")
            || requestPath.startsWith("/chargebee/portalSession"))
        {
            return mono {
                val sessionKey = request.headers["Authorization"]?.get(0)

                val userID = if (sessionKey != null) webSessionService.getIdFromSession(sessionKey) else null
                if (userID == null) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                if (!requestPath.endsWith("/$userID")) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                }

                chain.filter(exchange).awaitSingleOrNull()
            }
        }

        return mono {
            val authHeader = request.headers["Authorization"]?.get(0)
            if (authHeader == null || authHeader != centralApiConfig.auth) {
                response.statusCode = HttpStatus.UNAUTHORIZED
                return@mono null
            }

            chain.filter(exchange).awaitSingleOrNull()
        }
    }
}