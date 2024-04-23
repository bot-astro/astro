package space.astro.api.central.services.bot

import org.springframework.stereotype.Service
import space.astro.api.central.configs.BotShardConfig

@Service
class PodMetaCalculatorService(
    private val botShardConfig: BotShardConfig
) {
    fun calculateShardIdFromGuildId(guildID: Long): Long {
        return (guildID shr 22) % botShardConfig.totalShards
    }

    fun calculatePodIdFromGuildId(guildID: Long): Long {
        val shardId = calculateShardIdFromGuildId(guildID)
        val shardsPerPod = botShardConfig.totalShards.floorDiv(botShardConfig.totalPods)

        return shardId.floorDiv(shardsPerPod)
    }
}