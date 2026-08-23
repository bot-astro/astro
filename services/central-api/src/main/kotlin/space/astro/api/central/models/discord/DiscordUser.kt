package space.astro.api.central.models.discord

data class DiscordUser(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String?,
    val email: String?
)