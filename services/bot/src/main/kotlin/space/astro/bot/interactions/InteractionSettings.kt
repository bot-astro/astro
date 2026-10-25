package space.astro.bot.interactions

import net.dv8tion.jda.api.Permission
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * How the interaction gets acknowledged before the handler runs
 */
enum class Ack {
    DEFER_REPLY,
    DEFER_REPLY_EPHEMERAL,

    /**
     * Only for component interactions, edits the message the component is attached to
     */
    DEFER_EDIT,

    /**
     * The handler acknowledges the interaction itself (e.g. by replying with a modal)
     */
    NONE,
}

/**
 * Static requirements and behaviour of an interaction handler
 *
 * @property cooldown per user and per handler, [Duration.ZERO] to disable
 * @property developerOnly registered only in the developer guild (see [space.astro.bot.properties.DeveloperProperties])
 * and executable only by the developer user
 */
data class InteractionSettings(
    val ack: Ack = Ack.DEFER_REPLY_EPHEMERAL,
    val cooldown: Duration = 2.seconds,
    val memberPermissions: Set<Permission> = emptySet(),
    val botPermissions: Set<Permission> = emptySet(),
    val voteRequired: Boolean = false,
    val developerOnly: Boolean = false,
)
