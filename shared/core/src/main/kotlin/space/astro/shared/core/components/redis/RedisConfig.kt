package space.astro.shared.core.components.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Redis configuration variables
 *
 * @property cluster
 * @property uris
 */
@Configuration
@ConfigurationProperties(prefix = "io.redis")
class RedisConfig {

    var cluster = false
    var uris = "redis://localhost:6379"
    var password = ""
    var host = "localhost"
    var port = 6379
    var database = 0
}
