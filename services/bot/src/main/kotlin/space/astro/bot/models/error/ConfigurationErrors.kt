package space.astro.bot.models.error

object ConfigurationErrors {
    fun premiumVariables() = ConfigurationError(
        type = ConfigurationError.ConfigurationErrorType.PREMIUM,
        description = "Your are trying to use premium variables and your server isn't premium!",
    )
}