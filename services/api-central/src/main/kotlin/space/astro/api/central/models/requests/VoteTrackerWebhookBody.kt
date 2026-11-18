package space.astro.api.central.models.requests

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Subset of the Vote Tracker forwarding payload that we care about, `data` only matches the `VOTE_CREATED` event
 */
data class VoteTrackerWebhookBody(
    val eventType: String,
    val data: Data? = null,
) {
    data class Data(
        val userPlatformId: String? = null,
        /** ISO-8601 UTC timestamp */
        val voteExpiresAt: String? = null,
        @JsonProperty("is_test")
        val test: Boolean = false,
    )
}
