package space.astro.bot.exceptions

import space.astro.bot.models.error.ConfigurationError

class ConfigurationException(
    error: ConfigurationError
): Exception(error.toString())