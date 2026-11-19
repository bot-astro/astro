package space.astro.shared.core.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.models.database.AiUsageEntity

interface AiUsageRepository : MongoRepository<AiUsageEntity, String>
