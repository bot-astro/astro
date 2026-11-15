package space.astro.bot.events.listeners.discord.voice.handlers

/**
 * Result of a joined generator event, used as the `outcome` tag of the `astro.vc.created` metric
 */
enum class VCCreationOutcome {
    CREATED,
    SKIPPED,
    CONFIG_ERROR,
    NO_PERMISSION,
    FAILED,
    ;

    val tag = name.lowercase()
}
