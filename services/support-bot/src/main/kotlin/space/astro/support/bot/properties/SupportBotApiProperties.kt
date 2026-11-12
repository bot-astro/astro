package space.astro.support.bot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("support.bot.api")
data class SupportBotApiProperties(
    /** Value of the Authorization header that clients of the API must send */
    val auth: String,
)
