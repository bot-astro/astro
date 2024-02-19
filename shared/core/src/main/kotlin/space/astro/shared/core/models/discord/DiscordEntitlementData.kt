package space.astro.shared.core.models.discord

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class DiscordEntitlementData(
    val id: String,
    @JsonProperty("sku_id")
    val skuId: String,
    @JsonProperty("guild_id")
    val guildId: String?,
    @JsonProperty("ends_at")
    val endsAt: OffsetDateTime?,
)