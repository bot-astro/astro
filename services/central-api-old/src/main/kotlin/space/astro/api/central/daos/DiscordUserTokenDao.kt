package space.astro.api.central.daos

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.api.central.models.discord.DiscordAuthedUser

interface DiscordUserTokenDao : MongoRepository<DiscordAuthedUser, String> {}