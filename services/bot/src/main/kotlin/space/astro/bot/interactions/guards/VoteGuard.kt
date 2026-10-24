package space.astro.bot.interactions.guards

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionException
import space.astro.bot.ui.Links
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.clients.VoteTrackerClient

/**
 * Locks the interactions marked with `voteRequired` behind a vote, premium guilds skip the lock
 */
@Component
@Order(20)
class VoteGuard(
    private val voteTrackerClient: VoteTrackerClient,
    private val premiumManager: PremiumManager,
) : InteractionGuard {
    override suspend fun check(request: InteractionRequest) {
        if (!request.settings.voteRequired) {
            return
        }

        if (request.guildSettings != null && premiumManager.isGuildPremium(request.guildSettings)) {
            return
        }

        val hasVoted =
            withContext(Dispatchers.IO) {
                voteTrackerClient.hasActiveVote(request.member.id)
            }

        if (!hasVoted) {
            throw InteractionException("guard.vote", "link" to Links.VOTE)
        }
    }
}
