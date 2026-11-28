package space.astro.api.central.models.responses

import io.swagger.v3.oas.annotations.media.Schema

data class McpGrantResponse(
    val id: String,
    @field:Schema(description = "URL of the client metadata document")
    val clientId: String,
    val clientName: String,
    val scopes: List<String>,
    @field:Schema(format = "date-time")
    val createdAt: String,
    @field:Schema(format = "date-time", description = "Last request made with a token of the grant, null if never used")
    val lastUsedAt: String?,
)
