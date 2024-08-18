package space.astro.bot.core.ui

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData

object Messages {
    fun dashboardSettings(): MessageCreateData {
        return MessageCreateBuilder()
            .setEmbeds(Embeds.dashboardSettings())
            .setActionRow(Buttons.dashboard)
            .build()
    }
}