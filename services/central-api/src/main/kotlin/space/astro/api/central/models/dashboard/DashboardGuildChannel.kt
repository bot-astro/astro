package space.astro.api.central.models.dashboard

data class DashboardGuildChannel(
    val id: String,
    val name: String?,
    val type: Int,
    val parentID: String?,
    val parentName: String?
)
