package space.astro.bot.core.exceptions

import space.astro.bot.models.error.ConfigurationErrorDto

class ConfigurationException(
    val configurationErrorDto: ConfigurationErrorDto
): Exception(configurationErrorDto.toString())