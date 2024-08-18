package space.astro.bot.controllers.dashboard

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import space.astro.bot.components.managers.InterfaceManager
import space.astro.shared.core.components.web.BotApiRoutes
import space.astro.shared.core.models.dashboard.DashboardGuildChannel
import space.astro.shared.core.models.dashboard.DashboardGuildRole
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.InterfaceData
import space.astro.shared.core.models.database.PermissionsInherited

@RestController
class DashboardController(
    private val shardManager: ShardManager,
    private val interfaceManager: InterfaceManager
) {
    @GetMapping(BotApiRoutes.DASHBOARD.IS_BOT_IN_GUILD)
    suspend fun isBotInGuild(
        @PathVariable guildID: String
    ) : ResponseEntity<*> {
        shardManager.getGuildById(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        return ResponseEntity.status(200).build<Any>()
    }

    @GetMapping(BotApiRoutes.DASHBOARD.GUILD_CHANNELS)
    suspend fun getGuildChannels(
        @PathVariable guildID: String
    ) : ResponseEntity<*> {
        val guild = shardManager.getGuildById(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        val channels = guild.channels.map { channel ->
            val parent = if (channel is ICategorizableChannel) {
                channel.parentCategory
            } else {
                null
            }

            DashboardGuildChannel(
                id = channel.id,
                name = channel.name,
                type = channel.type.id,
                parentID = parent?.id,
                parentName = parent?.name
            )
        }

        return ResponseEntity.ok(channels)
    }

    @GetMapping(BotApiRoutes.DASHBOARD.GUILD_ROLES)
    suspend fun getGuildRoles(
        @PathVariable guildID: String
    ) : ResponseEntity<*> {
        val guild = shardManager.getGuildById(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        val roles = guild.roles.map { role ->
            DashboardGuildRole(
                id = role.id,
                name = role.name,
                color = role.colorRaw,
                position = role.position
            )
        }

        return ResponseEntity.ok(roles)
    }

    @GetMapping(BotApiRoutes.DASHBOARD.CREATE_GENERATOR)
    suspend fun createGenerator(
        @PathVariable guildID: String
    ) : ResponseEntity<*> {
        val guild = shardManager.getGuildById(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        try {
            val generatorChannel = guild.createVoiceChannel("➕ Generator").await()
            val generatorData = GeneratorData(
                id = generatorChannel.id,
                category = null,
                chatCategory = null,
                chatPermissionsInherited = PermissionsInherited.GENERATOR
            )

            return ResponseEntity.ok(generatorData)
        } catch (e: ErrorResponseException) {
            return ResponseEntity.status(405).body(e.message)
        } catch (e: InsufficientPermissionException) {
            return ResponseEntity.status(405).build<Any>()
        } catch (e: Exception) {
            return ResponseEntity.status(500).build<Any>()
        }
    }

    @GetMapping(BotApiRoutes.DASHBOARD.CREATE_INTERFACE)
    suspend fun createInterface(
        @PathVariable guildID: String,
        @PathVariable channelID: String
    ) : ResponseEntity<*> {
        val guild = shardManager.getGuildById(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        val channel = guild.getTextChannelById(channelID)
            ?: return ResponseEntity.notFound().build<Any>()

        try {
            val interfaceData = interfaceManager.createInterface(channel)

            return ResponseEntity.ok(interfaceData)
        } catch (e: ErrorResponseException) {
            return ResponseEntity.status(405).body(e.message)
        } catch (e: InsufficientPermissionException) {
            return ResponseEntity.status(405).build<Any>()
        } catch (e: Exception) {
            return ResponseEntity.status(500).build<Any>()
        }
    }

    @PostMapping(BotApiRoutes.DASHBOARD.UPDATE_INTERFACE)
    suspend fun updateInterface(
        @PathVariable guildID: String,
        @RequestBody interfaceData: InterfaceData
    ) : ResponseEntity<*> {
        val guild = shardManager.getGuildById(guildID)
            ?: return ResponseEntity.notFound().build<Any>()

        try {
            interfaceManager.updateInterface(guild, interfaceData)

            return ResponseEntity.ok(interfaceData)
        } catch (e: ErrorResponseException) {
            return ResponseEntity.status(405).body(e.message)
        } catch (e: InsufficientPermissionException) {
            return ResponseEntity.status(405).build<Any>()
        } catch (e: PermissionException) {
            return ResponseEntity.status(405).build<Any>()
        } catch (e: Exception) {
            return ResponseEntity.status(500).build<Any>()
        }
    }
}