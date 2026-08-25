package space.astro.shared.core.models.discord

/**
 * Discord data you get when exchanging a code for a token
 *
 * @param guild data of the guild the bot has been added to through the OAuth2 flow
 */
data class DiscordTokenDto(
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String,
    val scope: String?,
    val tokenType: String?,
    val guild: DiscordTokenGuildDto?
)

data class DiscordTokenGuildDto(
    val id: String,
    val name: String,
    val icon: String?
)