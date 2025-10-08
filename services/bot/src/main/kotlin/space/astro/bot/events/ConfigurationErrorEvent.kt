package space.astro.bot.events

import space.astro.shared.core.models.database.ConfigurationErrorData

class ConfigurationErrorEvent(
    val configurationErrorData: ConfigurationErrorData
)