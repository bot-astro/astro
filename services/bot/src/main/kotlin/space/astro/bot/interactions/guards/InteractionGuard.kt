package space.astro.bot.interactions.guards

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import space.astro.bot.interactions.InteractionException
import space.astro.bot.interactions.InteractionSettings
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

class InteractionRequest(
    /** slash command name or component / modal id, used to scope cooldowns */
    val key: String,
    val guild: Guild,
    val member: Member,
    val guildSettings: GuildSettingsEntity?,
    val settings: InteractionSettings,
)

/**
 * Check that runs before an interaction gets acknowledged, guards are ordered with `@Order`
 */
interface InteractionGuard {
    /**
     * @throws InteractionException if the interaction must not proceed, the message is shown to the user
     */
    suspend fun check(request: InteractionRequest)
}
