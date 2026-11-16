package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @param guardEnabled Whether the interactions marked with `voteRequired` are actually locked behind a vote
 */
@ConfigurationProperties("vote-tracker")
data class VoteTrackerProperties(
    val guardEnabled: Boolean,
)
