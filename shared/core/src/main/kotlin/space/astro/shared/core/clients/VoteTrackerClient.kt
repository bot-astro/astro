package space.astro.shared.core.clients

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import space.astro.shared.core.properties.VoteTrackerProperties
import java.time.Duration

private val log = KotlinLogging.logger { }

/**
 * Client of the [Vote Tracker](https://developer.votetracker.bot/scalar.html) integration API
 */
class VoteTrackerClient(
    private val voteTrackerProperties: VoteTrackerProperties,
) {
    private data class VoteStatus(
        @JsonProperty("vote_active") val voteActive: Boolean,
    )

    private val client =
        RestClient
            .builder()
            .baseUrl(voteTrackerProperties.baseUrl)
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(Duration.ofMillis(voteTrackerProperties.timeoutMillis.toLong()))
                    setReadTimeout(Duration.ofMillis(voteTrackerProperties.timeoutMillis.toLong()))
                },
            ).defaultHeader("Authorization", "Bearer ${voteTrackerProperties.token}")
            .build()

    /**
     * Whether the user has an active vote (the vote lock is open).
     *
     * Fails open: if Vote Tracker is unreachable the user is let through, so that an outage of it does not lock the bot
     */
    fun hasActiveVote(userId: String): Boolean =
        try {
            client
                .get()
                .uri("/api/v1/integrations/{integrationId}/votes/users/{userId}/status", voteTrackerProperties.integrationId, userId)
                .retrieve()
                .body(VoteStatus::class.java)
                ?.voteActive ?: false
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() == 404) {
                false
            } else {
                log.error(e) { "Failed fetching the vote status of user $userId from Vote Tracker" }
                true
            }
        } catch (e: RestClientException) {
            log.error(e) { "Failed fetching the vote status of user $userId from Vote Tracker" }
            true
        }
}
