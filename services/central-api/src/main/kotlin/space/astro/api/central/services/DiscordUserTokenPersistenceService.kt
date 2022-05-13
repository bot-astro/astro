package space.astro.api.central.services

import org.springframework.stereotype.Service
import space.astro.api.central.models.TokenPayloadDto

@Service
class DiscordUserTokenPersistenceService {

    fun updateCredentials(userId: Long, tokenPayloadDto: TokenPayloadDto) {
        TODO("not implemented")
    }
}