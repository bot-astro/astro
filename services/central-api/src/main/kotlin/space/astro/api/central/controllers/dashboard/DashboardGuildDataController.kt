package space.astro.api.central.controllers.dashboard

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import io.swagger.v3.oas.annotations.tags.Tag
import net.dv8tion.jda.api.Permission
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.models.dashboard.DashboardGuildDto
import space.astro.shared.core.components.web.CentralApiRoutes
import space.astro.api.central.util.getUserID
import space.astro.api.central.models.dashboard.body.GuildDataInterfaceCreateBody
import space.astro.api.central.models.dashboard.body.GuildDataSettingsBody
import space.astro.api.central.services.bot.PodMetaCalculatorService
import space.astro.api.central.services.dashboard.DashboardGuildsPersistenceService
import space.astro.api.central.services.discord.DiscordGuildsFetchService
import space.astro.api.central.util.getAccessToken
import space.astro.shared.core.components.managers.PremiumRequirementDetector
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.database.ConnectionData
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.InterfaceData
import space.astro.shared.core.models.database.TemplateData
import space.astro.shared.core.services.bot.BotApiService
import space.astro.shared.core.util.exceptions.BotApiPermissionException
import space.astro.shared.core.util.exceptions.NotFoundException

@RestController
@Tag(name = "dashboard-data")
class DashboardGuildDataController(
    private val guildDao: GuildDao,
    private val dashboardGuildsPersistenceService: DashboardGuildsPersistenceService,
    private val podMetaCalculatorService: PodMetaCalculatorService,
    private val botApiService: BotApiService,
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val discordGuildsFetchService: DiscordGuildsFetchService,
) {
    ///////////////////////////////
    /// GETTER FOR ALL SETTINGS ///
    ///////////////////////////////

    @GetMapping(CentralApiRoutes.Dashboard.GUILD_DATA)
    suspend fun getGuildData(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val accessToken = exchange.getAccessToken()

        val dashboardGuild = run {
            var guild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)

            if (guild == null) {
                val dashboardGuilds = discordGuildsFetchService.fetchGuilds(accessToken)
                    .map { partialGuild ->
                        DashboardGuildDto(
                            id = partialGuild.id,
                            name = partialGuild.name,
                            icon = partialGuild.icon,
                            canManage = Permission.getPermissions(partialGuild.permissions).any {
                                it == Permission.MANAGE_CHANNEL || it == Permission.MANAGE_SERVER || it == Permission.ADMINISTRATOR
                            },
                        )
                    }

                dashboardGuildsPersistenceService.updateUserGuilds(userID, dashboardGuilds)

                guild = dashboardGuilds.firstOrNull { it.id == guildID }
            }

            return@run guild
        } ?: return ResponseEntity.notFound().build<Any>()

        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val endpoint = podMetaCalculatorService.calculatePodEndpoint(guildID)
        val hasBot = botApiService.isBotInGuild(endpoint, guildID)
        if (!hasBot) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build<Any>()
        }

        val guildData = guildDao.getOrCreate(guildID)

        return ResponseEntity.ok(guildData)
    }

    ///////////////////////////////
    /// UPDATE GENERIC SETTINGS ///
    ///////////////////////////////

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_UPDATE_SETTINGS)
    suspend fun updateGuildSettings(
        @PathVariable guildID: String,
        @RequestBody guildSettings: GuildDataSettingsBody,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }


        val validation = guildSettings.validate()
        if (!validation.isValid) {
            return ResponseEntity.badRequest().body(validation.invalidMessage)
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        guildData.allowMissingAdminPerm = guildSettings.allowMissingAdminPerm

        guildDao.save(guildData)

        return ResponseEntity.ok(guildData)
    }


    //////////////////
    /// GENERATORS ///
    //////////////////

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_CREATE_GENERATOR)
    suspend fun createGuildGenerator(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val endpoint = podMetaCalculatorService.calculatePodEndpoint(guildID)

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        if (!premiumRequirementDetector.canCreateGenerator(guildData)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build<Any>()
        }

        try {
            val generatorData = botApiService.createGenerator(
                endpoint = endpoint,
                guildID = guildID
            )

            guildData.generators.add(generatorData)
            guildDao.save(guildData)
            return ResponseEntity.ok(guildData)
        } catch (e: BotApiPermissionException) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build<Any>()
        } catch (e: NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build<Any>()
        }
    }

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_SINGLE_GENERATOR)
    suspend fun updateGuildGenerator(
        @PathVariable guildID: String,
        @PathVariable generatorID: String,
        @RequestBody generatorData: GeneratorData,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        if (generatorData.commandsSettings.minBitrate < 8000) {
            generatorData.commandsSettings.minBitrate = 8000
        }

        val validation = generatorData.validate()
        if (!validation.isValid) {
            return ResponseEntity.badRequest().body(validation.invalidMessage)
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        val genIndex = guildData.generators.indexOfFirst { it.id == generatorID }
            .takeIf { it != -1 }
            ?: return ResponseEntity.notFound().build<Any>()

        guildData.generators[genIndex] = generatorData
        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }

    @DeleteMapping(CentralApiRoutes.Dashboard.GUILD_SINGLE_GENERATOR)
    suspend fun deleteGuildGenerator(
        @PathVariable guildID: String,
        @PathVariable generatorID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        guildData.generators.removeIf { it.id == generatorID }

        // maybe delete from Discord too? same thing for interfaces? not sure honestly

        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }


    //////////////////
    /// INTERFACES ///
    //////////////////

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_CREATE_INTERFACE)
    suspend fun createGuildInterface(
        @PathVariable guildID: String,
        @RequestBody guildDataInterfaceCreateBody: GuildDataInterfaceCreateBody,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val endpoint = podMetaCalculatorService.calculatePodEndpoint(guildID)

        val validation = guildDataInterfaceCreateBody.validate()
        if (!validation.isValid) {
            return ResponseEntity.badRequest().body(validation.invalidMessage)
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        if (!premiumRequirementDetector.canCreateInterface(guildData)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build<Any>()
        }

        try {
            val interfaceData = botApiService.createInterface(
                endpoint = endpoint,
                guildID = guildID,
                channelID = guildDataInterfaceCreateBody.channelID
            )

            guildData.interfaces.add(interfaceData)
            guildDao.save(guildData)
            return ResponseEntity.ok(guildData)
        } catch (e: BotApiPermissionException) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build<Any>()
        } catch (e: NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build<Any>()
        }
    }

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_SINGLE_INTERFACE)
    suspend fun updateGuildInterface(
        @PathVariable guildID: String,
        @PathVariable interfaceID: String,
        @RequestBody interfaceData: InterfaceData,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val endpoint = podMetaCalculatorService.calculatePodEndpoint(guildID)

        val validation = interfaceData.validate()
        if (!validation.isValid) {
            return ResponseEntity.badRequest().body(validation.invalidMessage)
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        val interfaceIndex = guildData.interfaces.indexOfFirst { it.messageID == interfaceID }
            .takeIf { it != -1 }
            ?: return ResponseEntity.notFound().build<Any>()

        try {
            botApiService.updateInterface(
                endpoint = endpoint,
                guildID = guildID,
                interfaceData = interfaceData
            )
            guildData.interfaces[interfaceIndex] = interfaceData
            guildDao.save(guildData)
            return ResponseEntity.ok(guildData)
        } catch (e: BotApiPermissionException) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build<Any>()
        } catch (e: NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build<Any>()
        }
    }

    @DeleteMapping(CentralApiRoutes.Dashboard.GUILD_SINGLE_INTERFACE)
    suspend fun deleteGuildInterface(
        @PathVariable guildID: String,
        @PathVariable interfaceID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        guildData.interfaces.removeIf { it.messageID == interfaceID }
        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }


    ///////////////////
    /// VOICE ROLES ///
    ///////////////////

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_CREATE_VOICE_ROLE)
    suspend fun createGuildVoiceRole(
        @PathVariable guildID: String,
        @RequestBody connectionData: ConnectionData.ConnectionDataReqBody,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        if (!premiumRequirementDetector.canCreateConnection(guildData)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build<Any>()
        }

        val connection = connectionData.toConnectionData()
        val validation = connection.validate()
        if (!validation.isValid) {
            return ResponseEntity.badRequest().body(validation.invalidMessage)
        }


        guildData.connections.add(connection)
        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_SINGLE_VOICE_ROLE)
    suspend fun updateGuildVoiceRole(
        @PathVariable guildID: String,
        @PathVariable channelID: String,
        @RequestBody connectionData: ConnectionData.ConnectionDataReqBody,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        val connection = connectionData.toConnectionData()
        val validation = connection.validate()
        if (!validation.isValid) {
            return ResponseEntity.badRequest().body(validation.invalidMessage)
        }

        val index = guildData.connections.indexOfFirst { it.id == channelID }
            .takeIf { it != -1 }
            ?: return ResponseEntity.notFound().build<Any>()

        guildData.connections[index] = connection
        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }

    @DeleteMapping(CentralApiRoutes.Dashboard.GUILD_SINGLE_VOICE_ROLE)
    suspend fun deleteGuildVoiceRole(
        @PathVariable guildID: String,
        @PathVariable channelID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        guildData.connections.removeIf { it.id == channelID }
        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }


    /////////////////
    /// TEMPLATES ///
    /////////////////

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_CREATE_TEMPLATE)
    suspend fun createGuildTemplate(
        @PathVariable guildID: String,
        @RequestBody templateData: TemplateData,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        if (!premiumRequirementDetector.canCreateTemplate(guildData)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build<Any>()
        }

        templateData.id = NanoIdUtils.randomNanoId()
        val validation = templateData.validate()
        if (!validation.isValid) {
            return ResponseEntity.badRequest().body(validation.invalidMessage)
        }

        guildData.templates.add(templateData)
        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }

    @PostMapping(CentralApiRoutes.Dashboard.GUILD_SINGLE_TEMPLATE)
    suspend fun updateGuildTemplate(
        @PathVariable guildID: String,
        @PathVariable templateID: String,
        @RequestBody templateData: TemplateData,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        val validation = templateData.validate()
        if (!validation.isValid) {
            return ResponseEntity.badRequest().body(validation.invalidMessage)
        }

        val index = guildData.templates.indexOfFirst { it.id == templateID }
            .takeIf { it != -1 }
            ?: return ResponseEntity.notFound().build<Any>()

        guildData.templates[index] = templateData
        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }

    @DeleteMapping(CentralApiRoutes.Dashboard.GUILD_SINGLE_TEMPLATE)
    suspend fun deleteGuildTemplate(
        @PathVariable guildID: String,
        @PathVariable templateID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()
        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()
        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.get(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        guildData.templates.removeIf { it.id == templateID }
        guildDao.save(guildData)
        return ResponseEntity.ok(guildData)
    }
}