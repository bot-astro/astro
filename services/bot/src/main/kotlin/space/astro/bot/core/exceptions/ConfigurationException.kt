package space.astro.bot.core.exceptions

import space.astro.shared.core.models.influx.ConfigurationErrorDto

class ConfigurationException(
    val configurationErrorDto: ConfigurationErrorDto
): Exception(configurationErrorDto.toString())