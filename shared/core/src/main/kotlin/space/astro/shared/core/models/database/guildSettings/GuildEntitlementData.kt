package space.astro.shared.core.models.database.guildSettings

/**
 * Entitlement owned by a guild
 *
 * @param id Id of the entitlement
 * @param skuId Id of the SKU of the entitlement
 * @param endsAt Expiration of the entitlement in epoch millis, NULL if it never expires
 */
data class GuildEntitlementData(
    val id: String,
    val skuId: String,
    val endsAt: Long?
)