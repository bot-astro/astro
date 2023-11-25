package space.astro.bot.events

import space.astro.bot.models.error.ConfigurationError

class ConfigurationErrorEvent(
    guildId: String,
    configurationError: ConfigurationError
)