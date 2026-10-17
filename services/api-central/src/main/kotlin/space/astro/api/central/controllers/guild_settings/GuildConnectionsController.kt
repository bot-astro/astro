package space.astro.api.central.controllers.guild_settings

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.database.guildSettings.ConnectionData
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
class GuildConnectionsController(
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_CONNECTIONS)
    @CanManageGuildMiddleware
    fun createConnection(
        @PathVariable guildId: String,
        @RequestBody connectionCreateBody: ConnectionData,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        guildSettings.connections.add(connectionCreateBody)
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_CONNECTION)
    @CanManageGuildMiddleware
    fun updateConnection(
        @PathVariable guildId: String,
        @PathVariable connectionId: String,
        @RequestBody updatedConnectionSettings: ConnectionData,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
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

    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_CONNECTION)
    @CanManageGuildMiddleware
    fun deleteConnection(
        @PathVariable guildId: String,
        @PathVariable connectionId: String,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        guildSettings.connections.removeIf { it.id == connectionId }
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }
}
