package space.astro.api.central.models.ai

import space.astro.shared.core.models.database.AiDiffEntry
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.Instant

/**
 * Proposal as shown to the surfaces (dashboard data part and bot replies)
 *
 * @param expiresAt ISO instant
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiProposalDto(
    val id: String,
    val action: String,
    val summary: String,
    val diff: List<AiDiffEntry>,
    val expiresAt: String,
) {
    companion object {
        fun from(proposal: AiProposal) =
            AiProposalDto(
                id = proposal.id,
                action = proposal.action,
                summary = proposal.summary,
                diff = proposal.diff,
                expiresAt = Instant.ofEpochMilli(proposal.expiresAt).toString(),
            )
    }
}
