package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @param token Vote Tracker integration API token
 * @param integrationId Id of the Vote Tracker integration of the bot
 * @param timeoutMillis Timeout of the vote status request, kept low to not exceed the 3 seconds Discord gives to acknowledge an interaction
 */
@ConfigurationProperties("vote-tracker")
data class VoteTrackerProperties(
    val token: String,
    val integrationId: String,
    val baseUrl: String = "https://developer.votetracker.bot",
    val timeoutMillis: Int = 2000,
)
