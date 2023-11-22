package space.astro.bot.models.error

data class ConfigurationError(
    val type: ConfigurationErrorType,
    val description: String,
    val priority: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class ConfigurationErrorType {
        PREMIUM
    }

    override fun toString(): String {
        return "$type: $description (priority: $priority)"
    }
}