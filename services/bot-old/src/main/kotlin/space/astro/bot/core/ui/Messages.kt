package space.astro.bot.core.ui

import net.dv8tion.jda.api.entities.SkuSnowflake
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.internal.entities.SkuSnowflakeImpl

object Messages {
    fun dashboardSettings(): MessageCreateData {
        return MessageCreateBuilder()
            .setEmbeds(Embeds.dashboardSettings())
            .setActionRow(Buttons.dashboard)
            .build()
    }

    val ultimateRequired = MessageCreateBuilder()
        .setEmbeds(Embeds.ultimateRequired)
        .setActionRow(Buttons.premiumRequired)
        .build()
}