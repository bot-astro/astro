package space.astro.shared.core.models.database.userSettings

data class UserEntitlementData(
    val id: String,
    val skuId: String,
    val endsAt: Long?
) {
    fun isActive(currentMillis: Long) = (endsAt == null) || (endsAt >= currentMillis)
}