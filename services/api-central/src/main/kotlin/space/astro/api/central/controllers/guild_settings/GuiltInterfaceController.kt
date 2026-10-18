package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.InterfaceCreateBody
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.api.bot.request.InterfaceCreateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Interfaces settings")
class GuiltInterfaceController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @Operation(summary = "Create interface")
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_INTERFACES)
    @CanManageGuildMiddleware
    fun createInterface(
        @PathVariable guildId: String,
        @RequestBody interfaceCreateBody: InterfaceCreateBody,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val interfaceData =
            botApiClient.createInterface(
                guildId,
                InterfaceCreateBotApiRequest(channelId = interfaceCreateBody.channelId),
            )

        guildSettings.interfaces.add(interfaceData)
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(summary = "Update interface")
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_INTERFACE)
    @CanManageGuildMiddleware
    fun updateInterface(
        @PathVariable guildId: String,
        @PathVariable interfaceId: String,
        @RequestBody updatedInterfaceData: InterfaceData,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val interfaceIndex =
            guildSettings.interfaces
                .indexOfFirst { it.messageID == interfaceId }
                .takeIf { it != -1 }
                ?: throw ANotFoundException("Interface with ID $interfaceId not found for guild with id $guildId")

        botApiClient.updateInterface(guildId, updatedInterfaceData)

        guildSettings.interfaces[interfaceIndex] = updatedInterfaceData
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(summary = "Delete interface")
    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_INTERFACE)
    @CanManageGuildMiddleware
    fun deleteInterface(
        @PathVariable guildId: String,
        @PathVariable interfaceId: String,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        guildSettings.interfaces
            .firstOrNull { it.messageID == interfaceId }
            ?.also {
                try {
                    botApiClient.deleteInterface(guildId, it.channelID, it.messageID)
                } catch (_: Exception) {
                    // we don't care if it fails deleting on Discord
                }
            }

        guildSettings.interfaces.removeIf { it.messageID == interfaceId }
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }
}
