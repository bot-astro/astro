package space.astro.api.central.models.requests

import io.swagger.v3.oas.annotations.media.Schema

data class GuildProfileUpdateBody(
    @field:Schema(description = "A null field resets it to the Astro default")
    val nickname: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
)
