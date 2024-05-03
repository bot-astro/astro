package space.astro.shared.core.models.discord

data class DiscordUserData(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String?
)
