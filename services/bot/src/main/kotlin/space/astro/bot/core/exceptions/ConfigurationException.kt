package space.astro.bot.core.exceptions

import space.astro.bot.models.error.ConfigurationError

class ConfigurationException(
    val configurationError: ConfigurationError
): Exception(configurationError.toString())