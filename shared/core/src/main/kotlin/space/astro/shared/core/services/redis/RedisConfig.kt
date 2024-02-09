package space.astro.shared.core.services.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Redis configuration variables
 *
 * @property cluster
 * @property host
 * @property port
 * @property password
 * @property database
 */
@Configuration
@ConfigurationProperties(prefix = "io.redis")
class RedisConfig {

    var cluster = false
    var host: String = "localhost"
    var port: Int = 6379
    var password: CharSequence? = null
    var database: Int = 0
}
