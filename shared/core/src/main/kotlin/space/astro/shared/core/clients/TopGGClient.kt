package space.astro.shared.core.clients

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.properties.TopGGProperties

/**
 * Client of the [top.gg](https://docs.top.gg/api/v1/projects) projects API
 */
class TopGGClient(
    topGGProperties: TopGGProperties,
) {
    private data class Metrics(
        @JsonProperty("server_count") val serverCount: Int,
        @JsonProperty("shard_count") val shardCount: Int,
    )

    private val client =
        RestClient
            .builder()
            .baseUrl(topGGProperties.baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${topGGProperties.token}")
            .build()

    /**
     * Replaces the statistics of the bot on top.gg
     *
     * @throws AUnknownException
     */
    fun pushMetrics(
        serverCount: Int,
        shardCount: Int,
    ) {
        try {
            client
                .patch()
                .uri("/api/v1/projects/@me/metrics")
                .body(Metrics(serverCount, shardCount))
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientResponseException) {
            throw AUnknownException("Failed to push the bot statistics to top.gg", e)
        }
    }
}
