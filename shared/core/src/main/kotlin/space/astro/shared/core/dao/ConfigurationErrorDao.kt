package space.astro.shared.core.dao

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.models.database.ConfigurationErrorData

interface ConfigurationErrorDao : MongoRepository<ConfigurationErrorData, String> {}