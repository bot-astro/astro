package space.astro.shared.core.properties.api_central

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("bot.shard.manager")
data class BotShardProperties(
    val totalShards: Int = 1,
    val totalPods: Int = 1,
    val loginFactor: Int = 1
)