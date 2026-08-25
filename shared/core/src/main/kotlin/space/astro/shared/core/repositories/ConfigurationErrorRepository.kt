package space.astro.shared.core.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.models.database.ConfigurationErrorEntity

interface ConfigurationErrorRepository : MongoRepository<ConfigurationErrorEntity, String> {}