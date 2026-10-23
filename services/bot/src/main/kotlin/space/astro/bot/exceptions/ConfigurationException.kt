package space.astro.bot.exceptions

import space.astro.shared.core.models.database.ConfigurationErrorEntity

class ConfigurationException(
    val configurationErrorEntity: ConfigurationErrorEntity,
) : Exception(configurationErrorEntity.description)
