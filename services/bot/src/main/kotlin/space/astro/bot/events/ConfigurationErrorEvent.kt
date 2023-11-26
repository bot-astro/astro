package space.astro.bot.events

import space.astro.bot.models.error.ConfigurationErrorDto

class ConfigurationErrorEvent(
    guildId: String,
    configurationErrorDto: ConfigurationErrorDto
)