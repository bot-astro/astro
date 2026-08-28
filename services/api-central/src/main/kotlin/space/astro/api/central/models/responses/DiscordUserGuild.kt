package space.astro.api.central.models.responses

data class DiscordUserGuild(
    val id: String,
    val name: String,
    val icon: String?,
    val permissions: Long,
    val canManage: Boolean
)
