package space.astro.bot.interactions.guards

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionException
import space.astro.bot.properties.DeveloperProperties

/**
 * Locks the interactions marked with `developerOnly` to the developer user, inside the developer guild
 */
@Component
@Order(10)
class DeveloperGuard(
    private val developerProperties: DeveloperProperties,
) : InteractionGuard {
    override suspend fun check(request: InteractionRequest) {
        if (!request.settings.developerOnly) {
            return
        }

        if (!developerProperties.enabled ||
            request.guild.id != developerProperties.guildId ||
            request.member.id != developerProperties.userId
        ) {
            throw InteractionException("guard.developer_only")
        }
    }
}
