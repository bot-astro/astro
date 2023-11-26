package space.astro.support.bot.components.discord

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("shard.manager")
class ShardManagerConfig {

    var totalShards: Int = 1
    var totalPods: Int = 1
    var loginFactor: Int = 1

}
