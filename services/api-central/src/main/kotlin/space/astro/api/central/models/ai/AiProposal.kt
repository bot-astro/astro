package space.astro.api.central.models.ai

import space.astro.shared.core.models.database.AiDiffEntry
import space.astro.shared.core.models.database.AiSurface

/**
 * A change prepared by a write tool of the assistant, nothing is written until a user applies it
 *
 * @param action name of the tool that created it
 * @param targetId id of the changed entity, null when the proposal creates a new one
 * @param payload JSON of the exact value that gets written when the proposal is applied
 * @param baseVersion hash of the target when the proposal was created, the apply is rejected if it changed since
 * @param expiresAt epoch millis
 */
data class AiProposal(
    val id: String,
    val guildId: String,
    val userId: String,
    val surface: AiSurface,
    val action: String,
    val summary: String,
    val targetId: String?,
    val payload: String?,
    val diff: List<AiDiffEntry>,
    val baseVersion: String?,
    val expiresAt: Long,
)
