package space.astro.bot.interactions

import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import space.astro.shared.core.lang.L

/**
 * Names and descriptions of slash commands, subcommands and options
 *
 * Discord requires a description in the default locale, [description] reads it from the catalog. The other locales
 * are attached by JDA through this [LocalizationFunction], that looks up the key
 * `commands.<command>.<subcommand>.<option>.description` (the same one used by [description]) for each locale.
 */
object CommandLocalization : LocalizationFunction {
    private const val PREFIX = "commands."

    /**
     * Description in the default locale, [path] is the dot separated names of the command, subcommand and option
     */
    fun description(path: String): String = L.t(L.default, "$PREFIX$path.description")

    override fun apply(localizationKey: String): Map<DiscordLocale, String> {
        val key = PREFIX + localizationKey

        return L.locales
            .filter { it != L.default }
            .mapNotNull { locale ->
                val discordLocale = DiscordLocale.from(locale)
                val pattern = L.findPattern(locale, key)

                if (discordLocale == DiscordLocale.UNKNOWN || pattern == null) null else discordLocale to pattern
            }.toMap()
    }
}
