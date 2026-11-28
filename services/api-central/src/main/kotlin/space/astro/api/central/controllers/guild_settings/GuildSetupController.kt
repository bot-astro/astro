package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.responses.GuildSetupResponse
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.APremiumRequiredException
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Setup")
class GuildSetupController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumManager: PremiumManager,
) {
    @Operation(
        operationId = "guilds.setup.run",
        summary = "Set up Astro, same as the /setup command",
        description =
            "Creates a category with a generator voice channel and an interface channel, and an updates channel if the guild has none, " +
                "then saves them in the guild settings. Returns the created channels and the updated settings. " +
                "Fails with 403 PREMIUM_REQUIRED if the guild already has the maximum number of generators or " +
                "interfaces of a free server. " +
                "Not idempotent: every call creates a new set of channels.",
        extensions = [
            Extension(
                properties = [
                    ExtensionProperty(
                        name = "x-forge-require-confirmation",
                        value = "This creates new channels in the Discord guild.",
                    ),
                ],
            ),
        ],
    )
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_SETUP)
    @CanManageGuildMiddleware
    fun setup(
        @PathVariable guildId: String,
    ): ResponseEntity<GuildSetupResponse> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        if (!premiumManager.canCreateGenerator(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_GENERATORS} generators")
        }

        if (!premiumManager.canCreateInterface(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_INTERFACES} interface")
        }

        val setup = botApiClient.setup(guildId)

        setup.updatesChannelData?.let { guildSettings.updatesChannel = it }
        guildSettings.interfaces.add(setup.interfaceData)
        guildSettings.generators.add(setup.generatorSettings)
        guildSettingsRepository.save(guildSettings)

        return ResponseEntity.ok(
            GuildSetupResponse(
                guildSettings = guildSettings,
                category = setup.category,
                interfaceChannel = setup.interfaceChannel,
                generatorChannel = setup.generatorChannel,
                updatesChannel = setup.updatesChannel,
            ),
        )
    }
}
