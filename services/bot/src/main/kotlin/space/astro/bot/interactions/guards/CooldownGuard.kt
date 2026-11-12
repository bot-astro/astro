package space.astro.bot.interactions.guards

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Member
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionCooldownService
import space.astro.bot.interactions.InteractionException
import space.astro.bot.interactions.InteractionSettings
import java.time.Instant

/**
 * Not an [InteractionGuard]: it runs before the guild settings are retrieved (so a user on cooldown costs no database read)
 */
@Component
class CooldownGuard(
    private val interactionCooldownService: InteractionCooldownService,
) {
    /**
     * @param key slash command name or component / modal id, used to scope cooldowns
     * @throws InteractionException if the member is still on cooldown
     */
    suspend fun check(
        key: String,
        member: Member,
        settings: InteractionSettings,
    ) {
        val remaining =
            withContext(Dispatchers.IO) {
                interactionCooldownService.acquire(member.id, key, settings.cooldown)
            }

        if (remaining.isPositive()) {
            val availableAt = Instant.now().plusMillis(remaining.inWholeMilliseconds).epochSecond
            throw InteractionException("guard.cooldown", "availableAt" to availableAt.toString())
        }
    }
}
