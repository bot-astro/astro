package space.astro.shared.core.models.discord

data class DiscordUserDto(
    val id: Long,
    val username: String,
    val discriminator: String,
    val avatar: String?
)
