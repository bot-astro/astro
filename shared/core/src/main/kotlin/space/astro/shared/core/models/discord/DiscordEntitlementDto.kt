package space.astro.shared.core.models.discord

import java.time.OffsetDateTime

data class DiscordEntitlementDto(
    val id: String,
    val skuId: String,
    val guildId: String?,
    val userId: String?,
    val endsAt: OffsetDateTime?,
)
