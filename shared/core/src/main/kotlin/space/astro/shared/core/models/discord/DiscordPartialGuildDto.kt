package space.astro.shared.core.models.discord

data class DiscordPartialGuildDto(
    val id: String,
    val name: String,
    val icon: String?,
    val permissions: Long
)