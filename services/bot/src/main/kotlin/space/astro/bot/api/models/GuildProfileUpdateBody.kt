package space.astro.bot.api.models

data class GuildProfileUpdateBody(
    val nickname: String?,
    val bio: String?,
    val avatar: String?,
    val banner: String?
)