package space.astro.bot.config

import net.dv8tion.jda.api.entities.Activity
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("discord.application")
class DiscordApplicationConfig {
    val applicationId: Long = 715621848489918495
    val token: String = "token"
    val activityType = "CUSTOM_STATUS"
    val activityText = "/help | astro-bot.space"

    fun getActivityType(): Activity.ActivityType = Activity.ActivityType.valueOf(activityType)
}
