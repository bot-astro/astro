package space.astro.shared.core.util.extention

/**
 * Capitalizes the first char of this String
 */
fun String.capitalize() = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

/**
 * Capitalizes the first char of this String and lowercase the rest of it
 */
fun String.lowercaseAndCapitalize() = this.lowercase().capitalize()