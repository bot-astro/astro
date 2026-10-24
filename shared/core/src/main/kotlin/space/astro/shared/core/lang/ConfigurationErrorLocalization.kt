package space.astro.shared.core.lang

import space.astro.shared.core.models.database.ConfigurationErrorEntity
import java.util.Locale

/**
 * @return the description of the error in the given [locale]
 */
fun ConfigurationErrorEntity.localizedDescription(locale: Locale): String =
    L.t(locale, code.translationKey, *args.map { it.key to it.value }.toTypedArray())
