package space.astro.api.central.services.ai

import org.springframework.stereotype.Service
import space.astro.api.central.models.ai.AiProposal
import space.astro.shared.core.models.database.AiAuditEntity
import space.astro.shared.core.models.database.AiSurface
import space.astro.shared.core.models.database.AiUsageEntity
import space.astro.shared.core.repositories.mongo.AiAuditRepository
import space.astro.shared.core.repositories.mongo.AiUsageRepository

/**
 * Traceability of the assistant: the usage of every turn and every applied proposal
 */
@Service
class AiAuditService(
    private val aiUsageRepository: AiUsageRepository,
    private val aiAuditRepository: AiAuditRepository,
) {
    fun recordUsage(
        guildId: String,
        userId: String,
        surface: AiSurface,
        model: String,
        inputTokens: Int,
        outputTokens: Int,
        cost: Double?,
    ) {
        aiUsageRepository.save(
            AiUsageEntity(
                guildId = guildId,
                userId = userId,
                surface = surface,
                model = model,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                cost = cost,
            ),
        )
    }

    fun recordApplied(
        proposal: AiProposal,
        userId: String,
    ) {
        aiAuditRepository.save(
            AiAuditEntity(
                guildId = proposal.guildId,
                userId = userId,
                surface = proposal.surface,
                proposalId = proposal.id,
                action = proposal.action,
                diff = proposal.diff,
            ),
        )
    }
}
