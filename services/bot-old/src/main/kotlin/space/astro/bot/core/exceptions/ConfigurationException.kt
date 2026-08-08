package space.astro.bot.core.exceptions

import space.astro.shared.core.models.database.ConfigurationErrorData

class ConfigurationException(
    val configurationErrorData: ConfigurationErrorData
): Exception(configurationErrorData.toString())