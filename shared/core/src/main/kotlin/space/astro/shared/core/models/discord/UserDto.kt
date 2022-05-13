package space.astro.shared.core.models.discord

data class UserDto(
    val id: Long,
    val username: String,
    val discriminator: String,
    val avatar: String?
)
