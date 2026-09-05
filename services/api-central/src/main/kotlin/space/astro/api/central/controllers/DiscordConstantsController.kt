package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.utils.DiscordPermissionMeta
import space.astro.api.central.components.OpenApiConfiguration
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
class DiscordConstantsController {

    @ApiResponses(
        ApiResponse(
            responseCode = "401",
            content = [Content(mediaType = "application/json", schema = Schema(ref = OpenApiConfiguration.ERROR_RESPONSE_SCHEMA))]
        ),
        ApiResponse(
            responseCode = "500",
            content = [Content(mediaType = "application/json", schema = Schema(ref = OpenApiConfiguration.ERROR_RESPONSE_SCHEMA))]
        )
    )
    @GetMapping(CentralApiEndpoint.DISCORD_CHANNEL_RELATED_PERMISSIONS)
    suspend fun getChannelRelatedPermissions() : ResponseEntity<List<DiscordPermissionMeta>> {
        return ResponseEntity.ok(DiscordPermissionMeta.channelsRelatedPermissions)
    }
}