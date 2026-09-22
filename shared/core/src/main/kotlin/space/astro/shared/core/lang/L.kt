package space.astro.shared.core.lang

import com.ibm.icu.text.MessageFormat
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.util.Locale

object L {
    private val json = JsonMapper.builder().build()

    private val locales: List<Locale>
    private val default: Locale = Locale.forLanguageTag("en-US")
    private val bundles: Map<Locale, Map<String, String>>

    init {
        val stream = L::class.java.classLoader
            .getResourceAsStream("i18n/locales.txt")
            ?: error("i18n/locales.txt missing; did gradle task 'processResources' run?")

        val languageTags = stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }

        locales = languageTags.map(Locale::forLanguageTag)

        bundles = locales.associateWith { locale ->
            val resourcePath = "i18n/${locale.toLanguageTag()}.json"

            val resource = javaClass.classLoader.getResourceAsStream(resourcePath)
                ?.bufferedReader()?.use { it.readText() }
                ?: error("$resourcePath missing, did gradle task 'processResources' run?")

            json.readValue<Map<String, String>>(resource)
        }
    }

    ////////////
    // LOCALE //
    ////////////

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


    /////////////////
    // TRANSLATION //
    /////////////////
    fun t(locale: Locale, key: String, vararg args: Pair<String, Any?>): String {
            val pattern = bundles[locale]?.get(key)
                ?: bundles.getValue(default)[key]
                ?: throw AException(
                    httpStatusCode = 500,
                    errorCode = AErrorCode.TRANSLATION,
                    message = "Missing translation key: $key",
                    cause = null
                )

        try {
            return MessageFormat(pattern, locale).format(args.toMap())
        } catch (e: Exception) {
            throw AException(
                httpStatusCode = 500,
                errorCode = AErrorCode.TRANSLATION,
                message = "Translation failed for key $key with args: ${args.contentToString()}",
                cause = e
            )
        }
    }
}