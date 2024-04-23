package space.astro.api.central.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("shard.manager")
class BotShardConfig {
    var totalShards: Int = 1
    var totalPods: Int = 1
    var loginFactor: Int = 1
}
