package space.astro.bot.models.error

// TODO: Better format? Didn't wanna go too crazy on this stuff and loose time on it though
data class ConfigurationErrorDto(
    val description: String,
) {
    enum class ConfigurationErrorCode {
        PREMIUM, PERMISSION
    }

    override fun toString(): String {
        return description
    }
}