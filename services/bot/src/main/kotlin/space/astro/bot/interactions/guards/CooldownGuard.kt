package space.astro.bot.interactions.guards

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionCooldownService
import space.astro.bot.interactions.InteractionException
import java.time.Instant

@Component
@Order(10)
class CooldownGuard(
    private val interactionCooldownService: InteractionCooldownService,
) : InteractionGuard {
    override suspend fun check(request: InteractionRequest) {
        val remaining =
            withContext(Dispatchers.IO) {
                interactionCooldownService.acquire(request.member.id, request.key, request.settings.cooldown)
            }

        if (remaining.isPositive()) {
            val availableAt = Instant.now().plusMillis(remaining.inWholeMilliseconds).epochSecond
            throw InteractionException("This action is on cooldown, you will be able to use it again <t:$availableAt:R>")
        }
    }
}
