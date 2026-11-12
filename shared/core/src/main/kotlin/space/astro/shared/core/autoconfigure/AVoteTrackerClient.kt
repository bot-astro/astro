package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import space.astro.shared.core.clients.VoteTrackerClient
import space.astro.shared.core.properties.VoteTrackerProperties

@AutoConfiguration
@EnableConfigurationProperties(VoteTrackerProperties::class)
@ConditionalOnProperty(
    prefix = "vote-tracker",
    name = ["token"],
)
class AVoteTrackerClient {
    @Bean
    @ConditionalOnMissingBean
    fun voteTrackerClient(voteTrackerProperties: VoteTrackerProperties) = VoteTrackerClient(voteTrackerProperties)
}
