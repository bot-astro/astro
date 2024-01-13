package space.astro.bot.events

import space.astro.shared.core.models.influx.ConfigurationErrorDto

class ConfigurationErrorEvent(
    val guildId: String,
    val configurationErrorDto: ConfigurationErrorDto
)