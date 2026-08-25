package space.astro.bot.events.models

import space.astro.shared.core.models.database.ConfigurationErrorEntity

class ConfigurationErrorEvent(
    val configurationErrorEntity: ConfigurationErrorEntity
)