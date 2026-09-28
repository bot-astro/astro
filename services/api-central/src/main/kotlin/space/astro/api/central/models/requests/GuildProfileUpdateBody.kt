package space.astro.api.central.models.requests

data class GuildProfileUpdateBody(
    val nick: String? = null,
    val bio: String? = null,
    val avatarUri: String? = null,
    val bannerUri: String? = null,
)
