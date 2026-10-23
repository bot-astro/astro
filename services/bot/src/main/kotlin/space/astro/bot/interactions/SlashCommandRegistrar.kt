package space.astro.bot.interactions

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class SlashCommandRegistrar(
    private val registry: InteractionRegistry,
) {
    /**
     * Bulk overwrites the global commands, call this by a single shard (usually shard `0`)
     */
    fun registerCommands(jda: JDA) {
        jda
            .updateCommands()
            .addCommands(registry.slash.values.map { it.data })
            .queue(
                { log.info { "Bulk overwrote ${it.size} commands" } },
                { log.error(it) { "Unable to bulk overwrite commands" } },
            )
    }
}
