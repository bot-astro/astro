package space.astro.shared.core.models.api.bot.response

data class DiscordGuildChannelBotApiResponse(
    val id: String,
    val name: String?,
    val type: Int,
    val parentId: String?,
    val parentName: String?
)
