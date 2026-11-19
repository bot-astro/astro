package space.astro.shared.core.models.discord

/**
 * @param permissions bitset of the role permissions, Discord sends it as a string
 */
data class DiscordGuildDto(
    val id: String,
    val ownerId: String,
    val roles: List<Role>,
) {
    data class Role(
        val id: String,
        val permissions: String,
    )
}
