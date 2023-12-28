package space.astro.shared.core.components.mongo

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * MongoDB configuration variables
 *
 * @property connectionString
 * @property database
 */
@Configuration
@ConfigurationProperties(prefix = "io.mongo")
class MongoConfig {

    var connectionString = "mongodb://localhost:27017"
    var database = "AstroDev"
}
