package space.astro.api.central.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("central.api")
class CentralApiConfig {
    var auth: String = "password"
}