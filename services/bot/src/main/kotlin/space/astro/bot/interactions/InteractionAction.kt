package space.astro.bot.interactions

import net.dv8tion.jda.api.Permission

/**
 * Generic Astro action
 *
 * @property premium
 * @property cooldown in milliseconds
 */
enum class InteractionAction(
    val premium: Boolean,
    val cooldown: Long,
    val botPermissions: List<Permission>
) {
    GENERIC(false, 0, emptyList()),

    VC_CHAT(true, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_LOGS(false, 2000, emptyList()),
    VC_CLAIM(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_TRANSFER(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_BAN(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_HIDE(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_INVITE(true, 5000, listOf(Permission.ADMINISTRATOR)),
    VC_LOCK(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_PERMIT(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_UNHIDE(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_UNLOCK(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_BITRATE(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_LIMIT(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_NAME(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_REGION(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_TEMPLATE(false, 2000, listOf(Permission.ADMINISTRATOR)),
    VC_WAITING_ROOM(true, 2000, listOf(Permission.ADMINISTRATOR)),

    // PREDASHBOARD
    SETTINGS(false, 2000, listOf(Permission.ADMINISTRATOR)),
    HIGH_COOLDOWN_SETTINGS(false, 5000, listOf(Permission.ADMINISTRATOR)),
    TEMPLATE_SETTINGS(true, 2000, listOf(Permission.ADMINISTRATOR)),
}