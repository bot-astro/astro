package space.astro.bot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Developer only commands are registered just in [guildId] and can be executed only by [userId],
 * they are disabled if either is blank
 */
@ConfigurationProperties("discord.developer")
data class DeveloperProperties(
    val guildId: String = "",
    val userId: String = "",
) {
    val enabled: Boolean get() = guildId.isNotBlank() && userId.isNotBlank()
}
