package space.astro.api.central.models.requests

data class GuildProfileUpdateBody(
    val nickname: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
)
