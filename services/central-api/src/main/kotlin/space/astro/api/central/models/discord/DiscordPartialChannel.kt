package space.astro.api.central.models.discord

import com.fasterxml.jackson.annotation.JsonProperty

data class DiscordPartialChannel(
    val id: String,
    val name: String?,
    val type: Int,
    @JsonProperty("parent_id")
    val parentID: String?
)