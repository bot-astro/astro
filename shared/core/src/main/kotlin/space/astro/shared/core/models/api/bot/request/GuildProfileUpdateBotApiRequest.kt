package space.astro.shared.core.models.api.bot.request

data class GuildProfileUpdateBotApiRequest(
    val nickname: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
)
