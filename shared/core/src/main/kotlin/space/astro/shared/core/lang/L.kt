package space.astro.shared.core.lang

import java.util.Locale

/**
 * Object that manages localization
 */
object L {
    private val locales: List<Locale>
    private val default: Locale = Locale.forLanguageTag("en-US")

    init {
        val stream = L::class.java.classLoader
            .getResourceAsStream("i18n/locales.txt")
            ?: error("i18n/locales.txt missing; did gradle task 'processResources' run?")

        val languageTags = stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }

        locales = languageTags.map(Locale::forLanguageTag)
    }

    fun isLocaleSupported(languageTag: String)
        = locales.contains(Locale.forLanguageTag(languageTag))

    fun closestSupportedLocale(languageTag: String): Locale {
        val requestedLocale = Locale.forLanguageTag(languageTag)

        // Try exact match, then progressive truncation: es-419 -> es, pt-BR -> pt
        Locale.lookup(listOf(Locale.LanguageRange(requestedLocale.toLanguageTag())), locales)
            ?.let { return it }

        // Same language, different region: pt-PT -> pt-BR (if only pt-BR is supported)
        locales.firstOrNull { it.language == requestedLocale.language }
            ?.let { return it }

        return default
    }
}