package space.astro.shared.core.models.database.guildSettings

import org.springframework.data.mongodb.core.mapping.Field

/**
 * Entitlement owned by a guild
 *
 * @param id Id of the entitlement
 * @param skuId Id of the SKU of the entitlement
 * @param endsAt Expiration of the entitlement in epoch millis, NULL if it never expires
 */
data class GuildEntitlementData(
    // nested documents keep the legacy `id` field, Spring would otherwise map a property named id to `_id`
    @Field("id")
    val id: String,
    val skuId: String,
    val endsAt: Long?,
)
