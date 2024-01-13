package space.astro.bot.interactions.handlers.command.impl.predashboard

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.GeneratorSettingsInteractionContext
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.CommandCategory
import space.astro.bot.interactions.handlers.command.SubCommand
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.PermissionsInherited

@Command(
    name = "generator",
    description = "",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class GeneratorCommand(
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val configurationErrorService: ConfigurationErrorService,
    private val guildDao: GuildDao
) : AbstractCommand() {

    /////////////
    /// BASIC ///
    /////////////
    @SubCommand(
        name = "create",
        description = "Create a temporary vc generator"
    )
    suspend fun create(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {
        if (!premiumRequirementDetector.canCreateGenerator(ctx.guildData)) {
            throw ConfigurationException(configurationErrorService.maximumAmountOfGenerator())
        }

        ctx.replyHandler.deferReply()

        val category = if (event.channelType == ChannelType.TEXT) (event.channel as TextChannel).parentCategory else null

        val generatorAction = ctx.guild.createVoiceChannel("➕ Generator")
        if (category != null)
            generatorAction.setParent(category)

        val generator = generatorAction.await()
        ctx.guildData.generators.add(GeneratorData(
            id = generator.id,
            category = category?.id,
            chatCategory = category?.id,
            chatPermissionsInherited = PermissionsInherited.GENERATOR)
        )

        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.default("Now users can join ${generator.asMention} to create temporary voice channels."),
            component = Buttons.Docs.generator
        )
    }

    @SubCommand(
        name = "delete",
        description = "Delete a generator"
    )
    fun delete(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "fallback-generator",
        description = "Sets a fallback generator in case one isn't able to create more vcs (category limit)"
    )
    fun fallbackGenerator(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    //////////
    /// VC ///
    //////////
    @SubCommand(
        name = "badwords",
        description = "Sets whether users can use badwords for their vc name",
        group = "vc",
        groupDescription = "0"
    )
    fun badwords(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "bitrate",
        description = "Set the default, minimum and maximum bitrate for temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    fun bitrate(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "category",
        description = "Set in which category temporary vcs should get generated",
        group = "vc",
        groupDescription = "0"
    )
    fun category(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "limit",
        description = "Set the default, minimum and maximum user limit for temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    fun limit(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "name",
        description = "\"Set the name for temporary voice channels or for a specific state of temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    fun name(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "permissions",
        description = "Set from where temporary vcs will inherit the permissions",
        group = "vc",
        groupDescription = "0"
    )
    fun permissions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "positions",
        description = "Set the exact place where temporary vcs will be created in a category",
        group = "vc",
        groupDescription = "0"
    )
    fun position(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "queue",
        description = "Enable queue mode which will fills existing temporary vcs before creating new ones",
        group = "vc",
        groupDescription = "0"
    )
    fun queue(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "region",
        description = "Set the region for temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    fun region(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "rename-conditions",
        description = "Set when a vc name should update",
        group = "vc",
        groupDescription = "0"
    )
    fun renameConditions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "state",
        description = "Set the default state (unlocked, locked or hidden) for temporary vcs",
        group = "vc",
        groupDescription = "0"
    )
    fun state(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }


    /////////////////
    /// OWNERSHIP ///
    /////////////////
    @SubCommand(
        name = "permissions",
        description = "Set specific permissions for the vc owners",
        group = "owner",
        groupDescription = "0"
    )
    fun ownerPermissions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "role",
        description = "Set a role that will be temporarily assigned to vc owners",
        group = "owner",
        groupDescription = "0"
    )
    fun ownerRole(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }


    ////////////
    /// CHAT ///
    ////////////
    @SubCommand(
        name = "category",
        description = "Set the category where private text chats get generated",
        group = "chat",
        groupDescription = "0"
    )
    fun chatCategory(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "creation",
        description = "Choose whether Astro should create private text chats or use the integrated voice text chats",
        group = "chat",
        groupDescription = "0"
    )
    fun chatCreation(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "message",
        description = "Create a message that will be sent in the private text chats",
        group = "chat",
        groupDescription = "0"
    )
    fun chatMessage(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "name",
        description = "The name for private text chats (see docs for variables)",
        group = "chat",
        groupDescription = "0"
    )
    fun chatName(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "nsfw",
        description = "Sets the nsfw setting for private text chats",
        group = "chat",
        groupDescription = "0"
    )
    fun chatNsfw(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "permissions",
        description = "Set from which channel private text chats should inherit the permissions",
        group = "chat",
        groupDescription = "0"
    )
    fun chatPermissions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "slowmode",
        description = "Set the slowmode for private text chat",
        group = "chat",
        groupDescription = "0"
    )
    fun chatSlowmode(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "topic",
        description = "Set the topic for private text chat",
        group = "chat",
        groupDescription = "0"
    )
    fun chatTopic(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }
}