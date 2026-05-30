package space.astro.api.central.services.bot

import org.springframework.stereotype.Service
import space.astro.api.central.configs.BotEndpointConfig
import space.astro.api.central.configs.BotShardConfig

@Service
class PodMetaCalculatorService(
    private val botShardConfig: BotShardConfig,
    private val botEndpointConfig: BotEndpointConfig
) {
    fun calculateShardIdFromGuildId(guildID: Long): Long {
        return (guildID shr 22) % botShardConfig.totalShards
    }

    fun calculatePodIdFromGuildId(guildID: Long): Long {
        val shardId = calculateShardIdFromGuildId(guildID)
        val shardsPerPod = botShardConfig.totalShards.floorDiv(botShardConfig.totalPods)

        return shardId.floorDiv(shardsPerPod)
    }

    fun calculatePodEndpoint(guildID: String): String {
        val podId = calculatePodIdFromGuildId(guildID.toLongOrNull()
            ?: throw IllegalArgumentException("guild id is not a long"))

        return if (botEndpointConfig.localhost) {
            "http://localhost:${botEndpointConfig.port}"
        } else {
            "http://${botEndpointConfig.podName}-$podId.${botEndpointConfig.serviceName}.${botEndpointConfig.namespace}:${botEndpointConfig.port}"
        }
    }
}