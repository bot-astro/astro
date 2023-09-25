package space.astro.bot.ui

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.MessageEmbed
import space.astro.shared.core.util.Colors
import space.astro.shared.core.util.Links
import space.astro.shared.core.util.extention.link

object Embeds {
    fun withDescription(description: String): MessageEmbed {
        return Embed(
            color = Colors.purple.rgb,
            description = description
        )
    }

    fun dashboardSettings() : MessageEmbed {
        return Embed(
            color = Colors.purple.rgb,
            description = "You can manage Astro's settings on its ${"dashboard".link(Links.dashboard)}!"
        )
    }
}