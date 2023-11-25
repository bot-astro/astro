package space.astro.support.bot.components.web

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import space.astro.shared.core.configs.SupportBotApiConfig

@Component
class CustomWebFilter(
    private val supportBotApiConfig: SupportBotApiConfig
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        response.headers.set("Access-Control-Allow-Origin", supportBotApiConfig.originUrl)
        response.headers.set("Access-Control-Allow-Methods", "*")
        response.headers.set("Access-Control-Allow-Headers", "*")

        // chain preflight requests
        if (request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }

        return mono {
            val authHeader = request.headers["Authorization"]?.get(0)
            if (authHeader == null || authHeader != supportBotApiConfig.auth) {
                response.statusCode = HttpStatus.UNAUTHORIZED
                return@mono null
            }

            chain.filter(exchange).awaitSingleOrNull()
        }
    }
}