package space.astro.bot.utils.extensions

import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import java.util.Locale

/**
 * Locale of the server, the only one used for translations: the closest supported one to the language set in the server settings,
 * the default locale if the server has no settings yet
 */
val GuildSettingsEntity?.supportedLocale: Locale
    get() = this?.let { L.closestSupportedLocale(it.locale) } ?: L.default
