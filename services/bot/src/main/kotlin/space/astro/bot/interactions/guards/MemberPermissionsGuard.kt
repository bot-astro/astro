package space.astro.bot.interactions.guards

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionException

@Component
@Order(30)
class MemberPermissionsGuard : InteractionGuard {
    override suspend fun check(request: InteractionRequest) {
        val required = request.settings.memberPermissions

        if (required.isNotEmpty() && !request.member.hasPermission(required)) {
            throw InteractionException(
                "guard.member_permissions",
                "permissions" to required.joinToString(", ") { it.getName() },
            )
        }
    }
}
