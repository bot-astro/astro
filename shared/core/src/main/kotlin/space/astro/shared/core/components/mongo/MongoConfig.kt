package space.astro.shared.core.components.mongo

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "io.mongo")
class MongoConfig {

    var connectionString = ""
    var database = ""
}
