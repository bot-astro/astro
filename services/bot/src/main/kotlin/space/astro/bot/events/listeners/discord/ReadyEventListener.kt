package space.astro.bot.events.listeners.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.interactions.SlashCommandRegistrar
import space.astro.bot.properties.DeveloperProperties

private val log = KotlinLogging.logger { }

@Component
class ReadyEventListener(
    private val slashCommandRegistrar: SlashCommandRegistrar,
    private val developerProperties: DeveloperProperties,
) {
    @EventListener
    fun onReady(event: ReadyEvent) {
        val shardId = event.jda.shardInfo.shardId

        log.info { "Logged in shard $shardId as ${event.jda.selfUser.name}" }

        if (shardId == 0) {
            log.info { "Upserting commands because we're on shard 0" }
            slashCommandRegistrar.registerCommands(event.jda)
        }
    }

    @EventListener
    fun onGuildReady(event: GuildReadyEvent) {
        if (developerProperties.enabled && event.guild.id == developerProperties.guildId) {
            slashCommandRegistrar.registerDeveloperCommands(event.guild)
        }
    }
}
