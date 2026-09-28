package space.astro.shared.core.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.models.database.MediaEntity

interface MediaRepository: MongoRepository<MediaEntity, String>