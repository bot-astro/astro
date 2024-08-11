package space.astro.shared.core.models.influx

data class ConfigurationErrorData(
    val guildId: String,
    val description: String,
    val premiumRequired: Boolean = false,
    val guide: Guide? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    enum class Guide {
        BASIC,
        GENERATOR,
        TEMPLATE,
        INTERFACE,
        VOICE_ROLE
    }
}