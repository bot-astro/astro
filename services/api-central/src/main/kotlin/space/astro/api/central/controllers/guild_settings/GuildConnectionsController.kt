package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.APremiumRequiredException
import space.astro.shared.core.models.database.guildSettings.ConnectionData
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Connections settings")
class GuildConnectionsController(
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumManager: PremiumManager,
) {
    @Operation(summary = "Create connection")
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_CONNECTIONS)
    @CanManageGuildMiddleware
    fun createConnection(
        @PathVariable guildId: String,
        @RequestBody connectionCreateBody: ConnectionData,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        if (!premiumManager.canCreateConnection(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_CONNECTIONS} voice role")
        }

        guildSettings.connections.add(connectionCreateBody)
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(summary = "Update connection")
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_CONNECTION)
    @CanManageGuildMiddleware
    fun updateConnection(
        @PathVariable guildId: String,
        @PathVariable connectionId: String,
        @RequestBody updatedConnectionSettings: ConnectionData,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val connectionIndex =
            guildSettings.connections
                .indexOfFirst { it.id == connectionId }
                .takeIf { it != -1 }
                ?: throw ANotFoundException("Connection with ID $connectionId not found in Astro database")

        guildSettings.connections[connectionIndex] = updatedConnectionSettings
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(summary = "Delete connection")
    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_CONNECTION)
    @CanManageGuildMiddleware
    fun deleteConnection(
        @PathVariable guildId: String,
        @PathVariable connectionId: String,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        guildSettings.connections.removeIf { it.id == connectionId }
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }
}
