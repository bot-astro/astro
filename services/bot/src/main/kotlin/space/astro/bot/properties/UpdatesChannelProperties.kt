package space.astro.bot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Announcement channels of the official Astro server that the updates channel of every guild follows,
 * a blank id is skipped
 */
@ConfigurationProperties("discord.updates-channel")
data class UpdatesChannelProperties(
    val announcementsChannelId: String = "",
    val statusChannelId: String = "",
)
