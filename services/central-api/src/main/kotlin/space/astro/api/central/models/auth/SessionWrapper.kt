package space.astro.api.central.models.auth

import space.astro.api.central.models.discord.OAuth2AuthorizationResponseDto

data class SessionWrapper(
    val id: String,
    val createdAt: Long,
    val data: OAuth2AuthorizationResponseDto
)
