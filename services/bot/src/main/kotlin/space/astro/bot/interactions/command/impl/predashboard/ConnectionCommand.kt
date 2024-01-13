package space.astro.bot.interactions.command.impl.predashboard

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.ConnectionSettingsInteractionContext
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.SettingsInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.database.ConnectionAction
import space.astro.shared.core.models.database.ConnectionData
import space.astro.shared.core.util.ui.Links

@Command(
    name = "connection",
    description = "Create, edit and delete connections",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.CONNECTION,
    action = InteractionAction.VC_NAME
)
class ConnectionCommand(
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val configurationErrorService: ConfigurationErrorService,
    private val guildDao: GuildDao
) : AbstractCommand() {
    @SubCommand(
        name = "create",
        description = "Assigns temporary roles to users who join specific audio channels or categories",
    )
    fun create(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext,
        @CommandOption(
            description = "The voice channel or category users need to join to get the role",
            type = OptionType.CHANNEL,
            channelTypes = [ChannelType.CATEGORY, ChannelType.VOICE, ChannelType.STAGE]
        )
        channel: GuildChannel,
        @CommandOption(
            description = "The role that users will get when joining that voice channel or category",
            type = OptionType.ROLE
        )
        role: Role,
        @CommandOption(
            description = "The action Astro should take with the role when the user joins the voice channel",
            type = OptionType.STRING,
        )
        action: ConnectionAction?,
        @CommandOption(
            description = "Whether the role action should be permanent instead of temporary",
            type = OptionType.BOOLEAN
        )
        permanent: Boolean?
    ) {
        if (!premiumRequirementDetector.canCreateConnection(ctx.guildData)) {
            throw ConfigurationException(configurationErrorService.maximumAmountOfConnections())
        }

        if (role.isPublicRole) {
            event.replyEmbeds(
                Embeds.error(
                "Astro cannot use the @everyone role in a connection since everyone in the server has that role by default and it cannot be removed." +
                "\n\nReuse this command and provide a valid role."
            )).setEphemeral(true)
                .queue()

            return
        }

        if (!ctx.guild.selfMember.canInteract(role)) {
            event.replyEmbeds(Embeds.error("The role ${role.asMention} is above Astro's role in the server role hierarchy." +
                    "\nBecause of that Astro cannot handle this role." +
                    "\nPut Astro's role above the provided one to fix this, see [this guide](${Links.ExternalGuides.ROLE_HIERARCHY})."))
                .setEphemeral(true)
                .queue()

            return
        }

        val connectionAction = action ?: ConnectionAction.ASSIGN

        connectionAction.permanent = permanent ?: false

        val connection = ConnectionData(
            channel.id,
            role.id,
            connectionAction
        )

        ctx.guildData.connections.add(connection)
        guildDao.save(ctx.guildData)

        event.replyEmbeds(Embeds.default(connection.toString()))
            .setEphemeral(true)
            .queue()
    }

    @SubCommand(
        name = "delete",
        description = "Deletes a connection",
    )
    fun delete(
        event: SlashCommandInteractionEvent,
        ctx: ConnectionSettingsInteractionContext
    ) {
        ctx.guildData.connections.remove(ctx.connectionData)

        guildDao.save(ctx.guildData)

        event.replyEmbeds(Embeds.default("The connection has been deleted"))
            .setEphemeral(true)
            .queue()
    }

    @SubCommand(
        name = "edit",
        description = "Change the role, channel and action of a connection"
    )
    fun edit(
        event: SlashCommandInteractionEvent,
        ctx: ConnectionSettingsInteractionContext,
        @CommandOption(
            description = "The voice channel or category users need to join to get the role",
            type = OptionType.CHANNEL,
            channelTypes = [ChannelType.CATEGORY, ChannelType.VOICE, ChannelType.STAGE]
        )
        channel: GuildChannel?,
        @CommandOption(
            description = "The role that users will get when joining that voice channel or category",
            type = OptionType.ROLE
        )
        role: Role?,
        @CommandOption(
            description = "The action Astro should take with the role when the user joins the voice channel",
            type = OptionType.STRING,
        )
        action: ConnectionAction?,
        @CommandOption(
            description = "Whether the role action should be permanent instead of temporary",
            type = OptionType.BOOLEAN
        )
        permanent: Boolean?
    ) {
        var updated = false

        if (channel != null && ctx.connectionData.id != channel.id) {
            ctx.connectionData.id = channel.id
            updated = true
        }

        if (role != null && ctx.connectionData.roleID != role.id) {
            if (role.isPublicRole) {
                event.replyEmbeds(
                    Embeds.error(
                        "Astro cannot use the @everyone role in a connection since everyone in the server has that role by default and it cannot be removed." +
                                "\n\nReuse this command and provide a valid role."
                    )).setEphemeral(true)
                    .queue()

                return
            }

            if (!ctx.guild.selfMember.canInteract(role)) {
                event.replyEmbeds(Embeds.error("The role ${role.asMention} is above Astro's role in the server role hierarchy." +
                        "\nBecause of that Astro cannot handle this role." +
                        "\nPut Astro's role above the provided one to fix this, see [this guide](${Links.ExternalGuides.ROLE_HIERARCHY})."))
                    .setEphemeral(true)
                    .queue()

                return
            }

            ctx.connectionData.roleID = role.id
            updated = true
        }

        if (action != null && ctx.connectionData.action != action) {
            ctx.connectionData.action = action
            updated = true
        }

        if (permanent != null && ctx.connectionData.action.permanent != permanent) {
            ctx.connectionData.action.permanent = permanent
            updated = true
        }

        if (updated) {
            ctx.guildData.connections.set(
                index = ctx.guildData.connections.indexOfFirst {
                    it.id == ctx.connectionData.id && it.roleID == ctx.connectionData.roleID
                },
                element = ctx.connectionData
            )

            guildDao.save(ctx.guildData)
        }

        event.replyEmbeds(
            Embeds.default("Connection edited:\n${ctx.connectionData}")
        )
    }
}