package space.astro.bot.interactions

/**
 * Generic Astro action
 *
 * @property premium
 * @property cooldown in milliseconds
 */
enum class InteractionAction(
    val premium: Boolean,
    val cooldown: Long
) {
    GENERIC(false, 0),

    VC_CHAT(true, 2000),
    VC_LOGS(false, 2000),
    VC_CLAIM(false, 2000),
    VC_TRANSFER(false, 2000),
    VC_BAN(false, 2000),
    VC_HIDE(false, 2000),
    VC_INVITE(true, 5000),
    VC_LOCK(false, 2000),
    VC_PERMIT(false, 2000),
    VC_UNHIDE(false, 2000),
    VC_UNLOCK(false, 2000),
    VC_BITRATE(false, 2000),
    VC_LIMIT(false, 2000),
    VC_NAME(false, 2000),
    VC_REGION(false, 2000),
    VC_TEMPLATE(false, 2000),
    VC_WAITING_ROOM(true, 2000)
}