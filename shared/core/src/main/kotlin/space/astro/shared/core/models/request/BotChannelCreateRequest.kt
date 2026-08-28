package space.astro.shared.core.models.request

data class BotChannelCreateRequest(
    val name: String,
    val categoryId: String?,
    val channelType: ChannelCreateType,
) {
    enum class ChannelCreateType {
        TEXT, VOICE, CATEGORY
    }
}