package space.astro.shared.core.models.discord

data class DiscordUserDto(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String?,
    val email: String?
)
