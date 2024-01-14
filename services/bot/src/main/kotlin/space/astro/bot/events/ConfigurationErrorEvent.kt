package space.astro.bot.events

import space.astro.shared.core.models.influx.ConfigurationErrorData

class ConfigurationErrorEvent(
    val guildId: String,
    val configurationErrorData: ConfigurationErrorData
)