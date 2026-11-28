package space.astro.api.central.models.responses

import io.swagger.v3.oas.annotations.media.Schema

data class AuthUserResponse(
    val id: String,
    val username: String,
    @field:Schema(description = "Discord avatar hash, the image is at https://cdn.discordapp.com/avatars/{id}/{avatar}.png")
    val avatar: String?,
)
