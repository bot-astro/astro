package space.astro.api.central.models.requests

import io.swagger.v3.oas.annotations.media.Schema

data class GuildUpgradeBody(
    @field:Schema(description = "Id of an active subscription with upgrades left, from the billing subscriptions endpoint")
    val subscriptionId: String,
)
