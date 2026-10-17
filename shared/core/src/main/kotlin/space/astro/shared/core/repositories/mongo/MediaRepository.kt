package space.astro.shared.core.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.models.database.MediaEntity

interface MediaRepository: MongoRepository<MediaEntity, String> {
    fun findAllByGuildId(guildId: String): MutableList<MediaEntity>
}