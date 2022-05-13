package space.astro.shared.core.components.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "io.redis")
class RedisConfig {

    var cluster = false
    var uris = "redis://localhost:6379"
}
