package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Generic configuration variables for web clients
 * @property httpMaxConnections
 * @property httpMaxIdleTime
 * @property httpMaxLifeTime
 * @property httpPendingAcquireTimeout
 * @property httpEvictInBackground
 */
@Configuration
@ConfigurationProperties("web.client")
class WebClientConfig {

    var httpMaxConnections = 100
    var httpMaxIdleTime: Long = 20
    var httpMaxLifeTime: Long = 60
    var httpPendingAcquireTimeout: Long = 20
    var httpEvictInBackground: Long = 120
}