package space.astro.bot.config

import net.dv8tion.jda.api.entities.Activity
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties("discord.application")
data class DiscordApplicationConfig(
    val id: Long,
    val token: String,
    val activityType: String,
    val activityText: String,
) {
    fun getActivityType(): Activity.ActivityType = Activity.ActivityType.valueOf(activityType)
}
