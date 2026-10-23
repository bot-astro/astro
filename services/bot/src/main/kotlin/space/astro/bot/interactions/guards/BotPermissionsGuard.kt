package space.astro.bot.interactions.guards

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionException

@Component
@Order(40)
class BotPermissionsGuard : InteractionGuard {
    override suspend fun check(request: InteractionRequest) {
        val required = request.settings.botPermissions

        if (required.isEmpty() || request.guildSettings?.allowMissingAdminPerm == true) {
            return
        }

        if (!request.guild.selfMember.hasPermission(required)) {
            throw InteractionException(
                "Astro needs the following permissions to run this command: ${required.joinToString(", ") { it.getName() }}",
            )
        }
    }
}
