package space.astro.bot.ui

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color

// TODO: replaced by components v2 helpers, see INTERACTION-MIGRATION.md step 5
object Embeds {
    private val purple = Color.decode("#B125EA")
    private val red = Color.decode("#F04747")

    fun default(description: String): MessageEmbed =
        EmbedBuilder()
            .setColor(purple)
            .setDescription(description)
            .build()

    fun error(description: String): MessageEmbed =
        EmbedBuilder()
            .setColor(red)
            .setDescription(description)
            .build()
}
