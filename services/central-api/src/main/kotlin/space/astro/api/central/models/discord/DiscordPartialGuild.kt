package space.astro.api.central.models.discord

data class DiscordPartialGuild(
    val id: String,
    val name: String,
    val icon: String?,
    val permissions: Long
)