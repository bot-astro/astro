package space.astro.bot.components.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant
import java.time.format.DateTimeParseException

data class AiTurnRequest(
    val userId: String,
    val threadId: String,
    val text: String,
)

data class AiUserRequest(
    val userId: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AiDiffEntry(
    val field: String,
    val before: String? = null,
    val after: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AiProposal(
    val id: String,
    val action: String = "",
    val summary: String = "",
    val diff: List<AiDiffEntry> = emptyList(),
    val expiresAt: String? = null,
) {
    /**
     * [expiresAt] as unix seconds, api-central can serialize it as an ISO instant or as epoch (seconds or milliseconds)
     */
    fun expiresAtEpochSeconds(): Long? {
        val raw = expiresAt?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        raw.toLongOrNull()?.let { return if (it > EPOCH_MILLIS_THRESHOLD) it / 1000 else it }

        return try {
            Instant.parse(raw).epochSecond
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private companion object {
        // seconds only reach this value in the year 5138
        const val EPOCH_MILLIS_THRESHOLD = 100_000_000_000L
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AiTurnResponse(
    val text: String = "",
    val proposals: List<AiProposal> = emptyList(),
    val limitReached: Boolean = false,
)

enum class AiApplyOutcome {
    APPLIED,

    /** the target changed since the proposal was made, or the proposal expired */
    CONFLICT,
    FORBIDDEN,
}
