package space.astro.api.central.models.dashboard

import space.astro.shared.core.models.database.GuildData

data class DashboardGuildDto(
    val id: String,
    val name: String,
    val icon: String?,
    val canManage: Boolean,
    val settings: GuildData?,
)