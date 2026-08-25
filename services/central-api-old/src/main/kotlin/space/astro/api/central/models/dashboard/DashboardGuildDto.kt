package space.astro.api.central.models.dashboard

data class DashboardGuildDto(
    val id: String,
    val name: String,
    val icon: String?,
    val canManage: Boolean,
)