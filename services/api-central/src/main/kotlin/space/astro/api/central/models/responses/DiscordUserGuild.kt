package space.astro.api.central.models.responses

import io.swagger.v3.oas.annotations.media.Schema

data class DiscordUserGuild(
    val id: String,
    val name: String,
    val icon: String?,
    @field:Schema(description = "Discord permissions bitfield of the user in the guild")
    val permissions: Long,
    @field:Schema(description = "Whether the user can manage the guild, only those can be configured")
    val canManage: Boolean,
)
