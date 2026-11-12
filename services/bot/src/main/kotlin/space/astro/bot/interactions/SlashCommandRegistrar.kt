package space.astro.bot.interactions

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import org.springframework.stereotype.Component
import space.astro.bot.properties.DeveloperProperties

private val log = KotlinLogging.logger { }

@Component
class SlashCommandRegistrar(
    private val registry: InteractionRegistry,
    private val developerProperties: DeveloperProperties,
) {
    /**
     * Bulk overwrites the global commands, call this by a single shard (usually shard `0`)
     */
    fun registerCommands(jda: JDA) {
        jda
            .updateCommands()
            .addCommands(
                registry.slash.values
                    .filter { !it.settings.developerOnly }
                    .map { it.data.setLocalizationFunction(CommandLocalization) },
            ).addCommands(
                registry.userContexts.values
                    .filter {
                        !it.settings.developerOnly
                    }.map { it.data.setLocalizationFunction(CommandLocalization) },
            ).queue(
                { log.info { "Bulk overwrote ${it.size} commands" } },
                { log.error(it) { "Unable to bulk overwrite commands" } },
            )
    }

    /**
     * Bulk overwrites the developer commands of [guild], call this by the shard that has the developer guild
     */
    fun registerDeveloperCommands(guild: Guild) {
        guild
            .updateCommands()
            .addCommands(
                registry.slash.values
                    .filter { it.settings.developerOnly }
                    .map { it.data.setLocalizationFunction(CommandLocalization) },
            ).addCommands(
                registry.userContexts.values.filter { it.settings.developerOnly }.map {
                    it.data.setLocalizationFunction(
                        CommandLocalization,
                    )
                },
            ).queue(
                { log.info { "Bulk overwrote ${it.size} developer commands in guild ${guild.id}" } },
                { log.error(it) { "Unable to bulk overwrite developer commands in guild ${guild.id}" } },
            )
    }
}
