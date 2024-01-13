package space.astro.shared.core.models.influx

/**
 * This is pretty simple
 * and could be improved with a more precise error reporting system in the future
 */
data class ConfigurationErrorDto(
    val description: String,
) {
    override fun toString(): String {
        return description
    }
}