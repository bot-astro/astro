package space.astro.api.central.models

import space.astro.shared.core.models.discord.UserDto

data class AuthorizationWrapperDto(
    val user: UserDto,
    val token: TokenPayloadDto
)
