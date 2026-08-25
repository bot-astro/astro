package space.astro.api.central.models.discord

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import space.astro.shared.core.models.discord.TokenPayloadDto

@Document(collection = "authedUsers")
data class DiscordAuthedUser(
    @Indexed(unique = true)
    val id: String,
    val discordAuthTokenInfo: TokenPayloadDto,
)