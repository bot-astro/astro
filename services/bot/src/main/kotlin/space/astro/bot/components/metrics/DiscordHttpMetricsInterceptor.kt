package space.astro.bot.components.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import okhttp3.Interceptor
import okhttp3.Response

class DiscordHttpMetricsInterceptor(
    private val registry: MeterRegistry,
) : Interceptor {
    private val snowflake = Regex("\\d{17,20}")
    private val interactionPath = Regex("interactions/\\{id}.*")
    private val webhookPath = Regex("webhooks/\\{id}.*")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        Counter
            .builder("http.discord.requests")
            .tag("method", request.method)
            .tag("status", response.code.toString())
            .tag("uri", normalize(request.url.toString()))
            .register(registry)
            .increment()

        return response
    }

    // keeps tag cardinality bounded by stripping ids and interaction/webhook tokens
    private fun normalize(url: String): String =
        url
            .replace(snowflake, "{id}")
            .replace(interactionPath, "interactions/{id}")
            .replace(webhookPath, "webhooks/{id}")
}
