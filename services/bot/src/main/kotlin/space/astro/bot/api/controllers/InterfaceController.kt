package space.astro.bot.api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.bot.components.InterfaceManager
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.models.api.bot.request.InterfaceCreateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.BotApiEndpoint

@RestController
@RequestMapping
@Tag(name = "Interface")
class InterfaceController(
    private val shardManager: ShardManager,
    private val interfaceManager: InterfaceManager,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @Operation(
        summary = "Create interface",
        description = "Sends a new interface message in the given channel and returns its data",
        operationId = "createInterface",
    )
    @PostMapping(BotApiEndpoint.DISCORD_GUILD_INTERFACE)
    suspend fun createInterface(
        @PathVariable guildId: String,
        @RequestBody request: InterfaceCreateBotApiRequest,
    ): ResponseEntity<InterfaceData> {
        val guild =
            shardManager.getGuildById(guildId)
                ?: throw ANotFoundException("Guild not found")

        try {
            val guildSettings = withContext(Dispatchers.IO) { guildSettingsRepository.findByGuildID(guildId) }

            return ResponseEntity.ok(interfaceManager.createInterface(guild, request.channelId, guildSettings.supportedLocale))
        } catch (e: AException) {
            throw e
        } catch (e: ErrorResponseException) {
            throw ABadRequestException(e.message ?: "Error from Discord API", e)
        } catch (e: InsufficientPermissionException) {
            throw ABadRequestException("Insufficient permissions to send the interface", e)
        } catch (e: Exception) {
            throw AUnknownException("An unexpected error occurred", e)
        }
    }

    @Operation(
        summary = "Update interface",
        description = "Edits the existing interface message to match the given data",
        operationId = "updateInterface",
    )
    @PutMapping(BotApiEndpoint.DISCORD_GUILD_INTERFACE)
    suspend fun updateInterface(
        @PathVariable guildId: String,
        @RequestBody interfaceData: InterfaceData,
    ): ResponseEntity<Void> {
        val guild =
            shardManager.getGuildById(guildId)
                ?: throw ANotFoundException("Guild not found")

        try {
            interfaceManager.updateInterface(guild, interfaceData)
            return ResponseEntity.noContent().build()
        } catch (e: AException) {
            throw e
        } catch (e: ErrorResponseException) {
            throw ABadRequestException(e.message ?: "Error from Discord API", e)
        } catch (e: InsufficientPermissionException) {
            throw ABadRequestException("Insufficient permissions to edit the interface", e)
        } catch (e: Exception) {
            throw AUnknownException("An unexpected error occurred", e)
        }
    }

    @Operation(
        summary = "Delete interface",
        description = "Deletes the interface message, succeeds if it is already gone",
        operationId = "deleteInterface",
    )
    @DeleteMapping(BotApiEndpoint.DISCORD_GUILD_INTERFACE_MESSAGE)
    suspend fun deleteInterface(
        @PathVariable guildId: String,
        @PathVariable channelId: String,
        @PathVariable messageId: String,
    ): ResponseEntity<Void> {
        val guild =
            shardManager.getGuildById(guildId)
                ?: throw ANotFoundException("Guild not found")

        try {
            interfaceManager.deleteInterface(guild, channelId, messageId)
            return ResponseEntity.noContent().build()
        } catch (e: AException) {
            throw e
        } catch (e: InsufficientPermissionException) {
            throw ABadRequestException("Insufficient permissions to delete the interface", e)
        } catch (e: Exception) {
            throw AUnknownException("An unexpected error occurred", e)
        }
    }
}
