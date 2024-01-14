package space.astro.shared.core.models.influx

import java.time.Instant

/**
 * This is pretty simple
 * and could be improved with a more precise error reporting system in the future
 */
data class ConfigurationErrorData(
    val description: String,
) {
    override fun toString(): String {
        return description
    }

    data class ConfigurationErrorWithInstantData(
        val description: String,
        val instant: Instant?
    )
}