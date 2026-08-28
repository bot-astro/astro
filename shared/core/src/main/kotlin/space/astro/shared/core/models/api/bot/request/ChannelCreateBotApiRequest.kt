package space.astro.shared.core.models.api.bot.request

data class ChannelCreateBotApiRequest(
    val name: String,
    val categoryId: String?,
    val channelType: ChannelCreateType,
) {
    enum class ChannelCreateType {
        TEXT, VOICE, CATEGORY
    }
}