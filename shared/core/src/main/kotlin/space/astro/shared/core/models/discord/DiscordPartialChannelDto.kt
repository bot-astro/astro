package space.astro.shared.core.models.discord

data class DiscordPartialChannelDto(
    val id: String,
    val name: String?,
    val type: Int,
    val parentID: String?,
    val parentName: String?
)
