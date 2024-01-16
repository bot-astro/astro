package space.astro.shared.core.models.discord

import java.time.OffsetDateTime

data class DiscordEntitlementData(
    val id: String,
    val skuId: String,
    val guildId: String?,
    val endsAt: OffsetDateTime?,
)