package space.astro.bot.models.error

data class ConfigurationError(
    val type: ConfigurationErrorType,
    val description: String,
) {
    enum class ConfigurationErrorType {
        PREMIUM
    }

    override fun toString(): String {
        return "$type: $description"
    }
}