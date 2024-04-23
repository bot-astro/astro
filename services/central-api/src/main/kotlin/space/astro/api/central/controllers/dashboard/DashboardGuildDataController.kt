package space.astro.api.central.controllers.dashboard

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings
import space.astro.api.central.configs.getUserID
import space.astro.api.central.services.dashboard.DashboardGuildsPersistenceService
import space.astro.shared.core.daos.GuildDao

@RestController
class DashboardGuildDataController(
    private val guildDao: GuildDao,
    private val dashboardGuildsPersistenceService: DashboardGuildsPersistenceService
) {
    ///////////////////////////////
    /// GETTER FOR ALL SETTINGS ///
    ///////////////////////////////

    @GetMapping(Mappings.Dashboard.GUILD_DATA)
    suspend fun getGuildData(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val userID = exchange.getUserID()

        val dashboardGuild = dashboardGuildsPersistenceService.getUserGuild(userID, guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        if (!dashboardGuild.canManage) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }

        val guildData = guildDao.getOrCreate(guildID)
        return ResponseEntity.ok(guildData)
    }


    ////////////////////////
    /// ULTIMATE UPGRADE ///
    ////////////////////////

    @GetMapping(Mappings.Dashboard.GUILD_UPGRADE)
    suspend fun upgradeGuild(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {

    }

    @GetMapping(Mappings.Dashboard.GUILD_DOWNGRADE)
    suspend fun downgradeGuild(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {

    }

    ///////////////////////////////
    /// UPDATE GENERIC SETTINGS ///
    ///////////////////////////////

    @PostMapping(Mappings.Dashboard.GUILD_UPDATE_SETTINGS)
    suspend fun updateGuildSettings(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {

    }


    //////////////////
    /// GENERATORS ///
    //////////////////

    @PostMapping(Mappings.Dashboard.GUILD_CREATE_GENERATOR)
    suspend fun createGuildGenerator(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {

    }

    @PostMapping(Mappings.Dashboard.GUILD_UPDATE_GENERATOR)
    suspend fun updateGuildGenerator(
        @PathVariable guildID: String,
        @PathVariable generatorID: String,
        exchange: ServerWebExchange
    ) {

    }


    //////////////////
    /// INTERFACES ///
    //////////////////

    @PostMapping(Mappings.Dashboard.GUILD_CREATE_INTERFACE)
    suspend fun createGuildInterface(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {

    }

    @PostMapping(Mappings.Dashboard.GUILD_UPDATE_INTERFACE)
    suspend fun updateGuildInterface(
        @PathVariable guildID: String,
        @PathVariable interfaceID: String,
        exchange: ServerWebExchange
    ) {

    }


    ///////////////////
    /// VOICE ROLES ///
    ///////////////////

    @PostMapping(Mappings.Dashboard.GUILD_CREATE_VOICE_ROLE)
    suspend fun createGuildVoiceRole(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {

    }

    @PostMapping(Mappings.Dashboard.GUILD_UPDATE_VOICE_ROLE)
    suspend fun updateGuildVoiceRole(
        @PathVariable guildID: String,
        @PathVariable channelID: String,
        exchange: ServerWebExchange
    ) {

    }


    /////////////////
    /// TEMPLATES ///
    /////////////////

    @PostMapping(Mappings.Dashboard.GUILD_CREATE_TEMPLATE)
    suspend fun createGuildTemplate(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {

    }

    @PostMapping(Mappings.Dashboard.GUILD_UPDATE_TEMPLATE)
    suspend fun updateGuildTemplate(
        @PathVariable guildID: String,
        @PathVariable templateID: String,
        exchange: ServerWebExchange
    ) {

    }
}