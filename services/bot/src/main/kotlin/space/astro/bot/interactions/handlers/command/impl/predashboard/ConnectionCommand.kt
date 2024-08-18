package space.astro.bot.interactions.handlers.command.impl.predashboard

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.shared.core.components.managers.PremiumRequirementDetector
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.core.ui.Messages
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.ConnectionSettingsInteractionContext
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.*
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.database.ConnectionAction
import space.astro.shared.core.models.database.ConnectionData
import space.astro.shared.core.util.ui.Links

@Command(
    name = "connection",
    description = "Manage voice roles",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class ConnectionCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        event.replyEmbeds(Embeds.default("You can configure connections (now called voice roles) in the ${Emojis.dashboard.formatted} [`Dashboard`](${Links.GUILD_DASHBOARD(ctx.guild.id)})"))
            .addActionRow(Buttons.guildDashboard(ctx.guild.id))
            .queue()
    }
//    @SubCommand(
//        name = "create",
//        description = "Assigns temporary roles to users who join specific audio channels or categories",
//    )
//    suspend fun create(
//        event: SlashCommandInteractionEvent,
//        ctx: SettingsInteractionContext,
//        @CommandOption(
//            description = "The voice channel or category users need to join to get the role",
//            type = OptionType.CHANNEL,
//            channelTypes = [ChannelType.CATEGORY, ChannelType.VOICE, ChannelType.STAGE]
//        )
//        channel: GuildChannel,
//        @CommandOption(
//            description = "The role that users will get when joining that voice channel or category",
//            type = OptionType.ROLE
//        )
//        role: Role,
//        @CommandOption(
//            description = "The action Astro should take with the role when the user joins the voice channel",
//            type = OptionType.STRING,
//        )
//        action: ConnectionAction?,
//        @CommandOption(
//            description = "Whether the role action should be permanent instead of temporary",
//            type = OptionType.BOOLEAN
//        )
//        permanent: Boolean?
//    ) {
//        if (!premiumRequirementDetector.canCreateConnection(ctx.guildData)) {
//            ctx.replyHandler.replyEmbedAndComponent(
//                embed = Embeds.error("There is already a Connection setup in this server.\nPremium is required to have more than one Connection." +
//                    "\nPossible solutions:" +
//                            "\n• Get ${Emojis.premium.formatted} Premium" +
//                            "\n• Delete the existing connection with `/connection delete`"
//                ),
//                component = Buttons.appDirectoryUltimate
//            )
//            return
//        }
//
//        if (role.isPublicRole) {
//            ctx.replyHandler.replyEmbed(
//                Embeds.error(
//                "Astro cannot use the @everyone role in a connection since everyone in the server has that role by default and it cannot be removed." +
//                "\n\nReuse this command and provide a valid role."
//            ))
//            return
//        }
//
//        if (!ctx.guild.selfMember.canInteract(role)) {
//            ctx.replyHandler.replyEmbed(Embeds.requireRoleHierarchy(role.name))
//            return
//        }
//
//        val connectionAction = action ?: ConnectionAction.ASSIGN
//
//        connectionAction.permanent = permanent ?: false
//
//        val connection = ConnectionData(
//            channel.id,
//            role.id,
//            connectionAction
//        )
//
//        ctx.guildData.connections.add(connection)
//        guildDao.save(ctx.guildData)
//
//        ctx.replyHandler.replyEmbed(Embeds.default(connection.toString()))
//    }
//
//    @SubCommand(
//        name = "delete",
//        description = "Deletes a connection",
//    )
//    suspend fun delete(
//        event: SlashCommandInteractionEvent,
//        ctx: ConnectionSettingsInteractionContext
//    ) {
//        ctx.guildData.connections.remove(ctx.connectionData)
//
//        guildDao.save(ctx.guildData)
//
//        ctx.replyHandler.replyEmbed(Embeds.default("The connection has been deleted"))
//    }
//
//    @SubCommand(
//        name = "edit",
//        description = "Change the role, channel and action of a connection"
//    )
//    suspend fun edit(
//        event: SlashCommandInteractionEvent,
//        ctx: ConnectionSettingsInteractionContext,
//        @CommandOption(
//            description = "The voice channel or category users need to join to get the role",
//            type = OptionType.CHANNEL,
//            channelTypes = [ChannelType.CATEGORY, ChannelType.VOICE, ChannelType.STAGE]
//        )
//        channel: GuildChannel?,
//        @CommandOption(
//            description = "The role that users will get when joining that voice channel or category",
//            type = OptionType.ROLE
//        )
//        role: Role?,
//        @CommandOption(
//            description = "The action Astro should take with the role when the user joins the voice channel",
//            type = OptionType.STRING,
//        )
//        action: ConnectionAction?,
//        @CommandOption(
//            description = "Whether the role action should be permanent instead of temporary",
//            type = OptionType.BOOLEAN
//        )
//        permanent: Boolean?
//    ) {
//        var updated = false
//
//        if (channel != null && ctx.connectionData.id != channel.id) {
//            ctx.connectionData.id = channel.id
//            updated = true
//        }
//
//        if (role != null && ctx.connectionData.roleID != role.id) {
//            if (role.isPublicRole) {
//                ctx.replyHandler.replyEmbed(
//                    Embeds.error(
//                        "Astro cannot use the @everyone role in a connection since everyone in the server has that role by default and it cannot be removed." +
//                                "\n\nReuse this command and provide a valid role."
//                    ))
//                return
//            }
//
//            if (!ctx.guild.selfMember.canInteract(role)) {
//                ctx.replyHandler.replyEmbed(Embeds.requireRoleHierarchy(role.name))
//                return
//            }
//
//            ctx.connectionData.roleID = role.id
//            updated = true
//        }
//
//        if (action != null && ctx.connectionData.action != action) {
//            ctx.connectionData.action = action
//            updated = true
//        }
//
//        if (permanent != null && ctx.connectionData.action.permanent != permanent) {
//            ctx.connectionData.action.permanent = permanent
//            updated = true
//        }
//
//        if (updated) {
//            ctx.guildData.connections[ctx.connectionIndex] = ctx.connectionData
//            guildDao.save(ctx.guildData)
//        }
//
//        ctx.replyHandler.replyEmbed(
//            Embeds.default("Connection edited:\n${ctx.connectionData}")
//        )
//    }
}