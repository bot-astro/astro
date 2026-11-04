package space.astro.shared.core.models.api.bot.request

import space.astro.shared.core.models.database.guildSettings.InterfaceData

data class InterfaceMoveBotApiRequest(
    val interfaceData: InterfaceData,
    val channelId: String,
)
