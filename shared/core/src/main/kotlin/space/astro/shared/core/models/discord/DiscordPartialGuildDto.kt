package space.astro.shared.core.models.discord

import com.fasterxml.jackson.annotation.JsonProperty

data class DiscordPartialGuildDto(
    val id: String,
    val name: String,
    val icon: String?,
    val permissions: Long,
    @JsonProperty("preferred_locale")
    val preferredLocale: String,
)