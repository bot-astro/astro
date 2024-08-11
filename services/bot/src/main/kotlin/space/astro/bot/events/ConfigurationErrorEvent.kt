package space.astro.bot.events

import space.astro.shared.core.models.influx.ConfigurationErrorData

class ConfigurationErrorEvent(
    val configurationErrorData: ConfigurationErrorData
)