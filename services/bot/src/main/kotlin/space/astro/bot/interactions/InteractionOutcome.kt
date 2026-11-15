package space.astro.bot.interactions

/**
 * Result of an interaction run, used as the `outcome` tag of the `astro.interactions` metric
 */
enum class InteractionOutcome {
    OK,
    OUTSIDE_GUILD,
    USER_ERROR,
    CONFIG_ERROR,
    NO_PERMISSION,
    FAILED,
    ;

    val tag = name.lowercase()
}
