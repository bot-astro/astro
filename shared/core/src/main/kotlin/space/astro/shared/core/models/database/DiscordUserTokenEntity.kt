package space.astro.shared.core.models.database

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import space.astro.shared.core.models.discord.DiscordTokenDto

/**
 * @property userId
 * @property accessToken
 * @property expiresIn how long, in seconds, until the access token expires
 * @property refreshToken
 * @property scope
 * @property tokenType
 */
@Document(collection = "discord_user_tokens")
data class DiscordUserTokenEntity(
    @Id
    val userId: String,
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String,
    val scope: String?,
    val tokenType: String?,
) {
    companion object {
        fun fromTokenPayload(
            userId: String,
            discordTokenDto: DiscordTokenDto,
        ) = DiscordUserTokenEntity(
            userId = userId,
            accessToken = discordTokenDto.accessToken,
            expiresIn = discordTokenDto.expiresIn,
            refreshToken = discordTokenDto.refreshToken,
            scope = discordTokenDto.scope,
            tokenType = discordTokenDto.tokenType
        )
    }
}