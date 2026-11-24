package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.InterfaceCreateBody
import space.astro.api.central.models.requests.InterfaceMoveBody
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.APremiumRequiredException
import space.astro.shared.core.models.api.bot.request.InterfaceCreateBotApiRequest
import space.astro.shared.core.models.api.bot.request.InterfaceMoveBotApiRequest
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Interfaces settings")
class GuiltInterfaceController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumManager: PremiumManager,
) {
    @Operation(
        operationId = "guilds.interfaces.create",
        summary = "Create interface",
        description =
            "Sends the interface message, with the buttons members use to control their temporary VC, " +
                "in the given text channel and registers it. Returns the updated guild settings, the new interface is " +
                "the last of `interfaces`. " +
                "Fails with 403 PREMIUM_REQUIRED if the guild reached the interfaces limit of a free server.",
        extensions = [
            Extension(
                properties = [
                    ExtensionProperty(
                        name = "x-forge-require-confirmation",
                        value = "This sends a new message in a channel of the Discord guild.",
                    ),
                ],
            ),
        ],
    )
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_INTERFACES)
    @CanManageGuildMiddleware
    fun createInterface(
        @PathVariable guildId: String,
        @RequestBody interfaceCreateBody: InterfaceCreateBody,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        if (!premiumManager.canCreateInterface(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_INTERFACES} interface")
        }

        val interfaceData =
            botApiClient.createInterface(
                guildId,
                InterfaceCreateBotApiRequest(channelId = interfaceCreateBody.channelId),
            )

        guildSettings.interfaces.add(interfaceData)
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        operationId = "guilds.interfaces.update",
        summary = "Update interface",
        description =
            "Replaces ALL the settings of an interface (embed style and buttons) with the request body, it is not a partial update: " +
                "get the current interface from the guild settings, change the fields you need and send the whole object back. " +
                "Edits the interface message on Discord. Changing the embed style or the order of the buttons needs premium " +
                "(403 PREMIUM_REQUIRED). Returns the updated guild settings. Idempotent.",
    )
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_INTERFACE)
    @CanManageGuildMiddleware
    fun updateInterface(
        @PathVariable guildId: String,
        @PathVariable interfaceId: String,
        @RequestBody updatedInterfaceData: InterfaceData,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val interfaceIndex =
            guildSettings.interfaces
                .indexOfFirst { it.messageID == interfaceId }
                .takeIf { it != -1 }
                ?: throw ANotFoundException("Interface with ID $interfaceId not found for guild with id $guildId")

        val currentInterface = guildSettings.interfaces[interfaceIndex]

        if (currentInterface.embedStyle != updatedInterfaceData.embedStyle &&
            !premiumManager.canEditInterfaceMessage(guildSettings)
        ) {
            throw APremiumRequiredException("Upgrade the server to edit the interface message")
        }

        if (currentInterface.buttonPositions() != updatedInterfaceData.buttonPositions() &&
            !premiumManager.canEditInterfaceButtonOrder(guildSettings)
        ) {
            throw APremiumRequiredException("Upgrade the server to edit the order of the interface buttons")
        }

        botApiClient.updateInterface(guildId, updatedInterfaceData)

        guildSettings.interfaces[interfaceIndex] = updatedInterfaceData
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        operationId = "guilds.interfaces.move",
        summary = "Move interface to another channel",
        description =
            "Sends the interface message again in another text channel. The interface gets a new id (its message id), " +
                "read it from the returned guild settings. Returns the updated guild settings.",
        extensions = [
            Extension(
                properties = [
                    ExtensionProperty(
                        name = "x-forge-require-confirmation",
                        value = "This sends the interface message in another channel, it gets a new id.",
                    ),
                ],
            ),
        ],
    )
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_INTERFACE_MOVE)
    @CanManageGuildMiddleware
    fun moveInterface(
        @PathVariable guildId: String,
        @PathVariable interfaceId: String,
        @RequestBody interfaceMoveBody: InterfaceMoveBody,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val interfaceIndex =
            guildSettings.interfaces
                .indexOfFirst { it.messageID == interfaceId }
                .takeIf { it != -1 }
                ?: throw ANotFoundException("Interface with ID $interfaceId not found for guild with id $guildId")

        val currentInterface = guildSettings.interfaces[interfaceIndex]

        // the interface message is re-sent in the new channel, so it gets a new message id
        guildSettings.interfaces[interfaceIndex] =
            botApiClient.moveInterface(
                guildId,
                InterfaceMoveBotApiRequest(interfaceData = currentInterface, channelId = interfaceMoveBody.channelId),
            )
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        operationId = "guilds.interfaces.delete",
        summary = "Delete interface",
        description =
            "Removes the interface from the guild settings and deletes its message on Discord " +
                "(a failure to delete the message is ignored). Returns the updated guild settings.",
        extensions = [
            Extension(
                properties = [
                    ExtensionProperty(
                        name = "x-forge-require-confirmation",
                        value = "This deletes the interface message on Discord.",
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_INTERFACE)
    @CanManageGuildMiddleware
    fun deleteInterface(
        @PathVariable guildId: String,
        @PathVariable interfaceId: String,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
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

private fun InterfaceData.buttonPositions() = buttons.associate { it.id to it.position }
