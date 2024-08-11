package space.astro.bot.components.web

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import space.astro.shared.core.components.web.BotApiRoutes
import space.astro.shared.core.configs.BotApiConfig
import space.astro.shared.core.configs.KubeConfig

@Component
class AuthWebFilter(
    private val kubeConfig: KubeConfig,
    private val botApiConfig: BotApiConfig,
): WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response
        val requestPath = request.path.toString()

        // chain preflight requests
        if (request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }

        if (requestPath.startsWith(BotApiRoutes.Kube.READY) || requestPath.startsWith(BotApiRoutes.Kube.SHUTDOWN) || requestPath.startsWith(BotApiRoutes.Kube.LIVENESS)) {
            return mono {
                val auth = request.headers["Authorization"]?.get(0)

                if (auth != kubeConfig.lifecycleAuthorization) {
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    return@mono null
                } else {
                    chain.filter(exchange).awaitSingleOrNull()
                }
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

            if (authHeader != botApiConfig.auth) {
                response.statusCode = HttpStatus.UNAUTHORIZED
                return@mono null
            }

            chain.filter(exchange).awaitSingleOrNull()
        }
    }
}
