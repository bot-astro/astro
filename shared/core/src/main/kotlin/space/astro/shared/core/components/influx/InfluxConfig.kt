package space.astro.shared.core.components.influx

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * InfluxDB configuration variables
 *
 * @property url
 * @property token
 * @property org
 * @property bucket
 */
@Configuration
@ConfigurationProperties(prefix = "io.influx")
class InfluxConfig {

    var url = ""
    var token = ""
    var org = ""
    var bucket = ""
}