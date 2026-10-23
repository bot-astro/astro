package space.astro.bot.interactions.guards

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(20)
class VoteGuard : InteractionGuard {
    override suspend fun check(request: InteractionRequest) {
        if (!request.settings.voteRequired) {
            return
        }

        // TODO: check that the user voted (top.gg) and throw an InteractionException with a vote prompt otherwise
    }
}
