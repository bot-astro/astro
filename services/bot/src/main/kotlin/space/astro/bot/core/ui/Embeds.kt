package space.astro.bot.core.ui

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.MessageEmbed
import space.astro.shared.core.util.extention.linkFromName
import space.astro.shared.core.util.ui.Colors
import space.astro.shared.core.util.ui.Links

object Embeds {
    fun default(description: String): MessageEmbed {
        return Embed(
            color = Colors.purple.rgb,
            description = description
        )
    }

    fun error(description: String): MessageEmbed {
        return Embed(
            color = Colors.red.rgb,
            description = description
        )
    }

    fun dashboardSettings() : MessageEmbed {
        return Embed(
            color = Colors.purple.rgb,
            description = "You can manage Astro's settings on its ${"dashboard".linkFromName(Links.DASHBOARD)}!"
        )
    }
}