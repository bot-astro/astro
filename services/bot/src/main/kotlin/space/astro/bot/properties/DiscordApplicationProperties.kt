package space.astro.bot.properties

import net.dv8tion.jda.api.entities.Activity
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("discord.application")
data class DiscordApplicationProperties(
    val id: Long,
    val token: String,
    val activityType: String,
    val activityText: String,
) {
    fun getActivityType(): Activity.ActivityType = Activity.ActivityType.valueOf(activityType)
}
