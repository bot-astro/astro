package space.astro.bot.interactions.handlers.command.impl.predashboard
//
//import net.dv8tion.jda.api.Permission
//import net.dv8tion.jda.api.entities.channel.ChannelType
//import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
//import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
//import net.dv8tion.jda.api.interactions.commands.OptionType
//import space.astro.shared.core.components.managers.PremiumRequirementDetector
//import space.astro.bot.core.ui.Buttons
//import space.astro.bot.core.ui.Embeds
//import space.astro.bot.core.ui.Emojis
//import space.astro.bot.interactions.InteractionAction
//import space.astro.bot.interactions.context.GeneratorSettingsInteractionContext
//import space.astro.bot.interactions.context.InteractionContext
//import space.astro.bot.interactions.handlers.command.*
//import space.astro.shared.core.daos.GuildDao
//import space.astro.shared.core.models.database.InitialPosition
//import space.astro.shared.core.models.database.PermissionsInherited
//import space.astro.shared.core.util.ui.Links
//
//@Command(
//    name = "generator-waiting",
//    description = "Manage temporary voice ",
//    requiredPermissions = [Permission.MANAGE_CHANNEL],
//    category = CommandCategory.SETTINGS,
//    action = InteractionAction.SETTINGS
//)
//class GeneratorWaitingCommand: AbstractCommand() {
//
//    @BaseCommand
//    suspend fun run(
//        event: SlashCommandInteractionEvent,
//        ctx: InteractionContext
//    ) {
//        event.replyEmbeds(Embeds.default("You can configure temporary voice channel generators in the ${Emojis.dashboard.formatted} [`Dashboard`](${Links.GUILD_DASHBOARD(ctx.guild.id)})"))
//            .addActionRow(Buttons.guildDashboard(ctx.guild.id))
//            .queue()
//    }
//
//    @SubCommand(
//        name = "bitrate",
//        description = "Set the default bitrate for waiting rooms"
//    )
//    suspend fun bitrate(
//        event: SlashCommandInteractionEvent,
//        ctx: GeneratorSettingsInteractionContext,
//        @CommandOption(
//            description = "The bitrate for waiting vcs (in kbps)",
//            type = OptionType.INTEGER,
//            minValue = 8
//        )
//        bitrate: Int
//    ) {
//        val guildMax = ctx.guild.maxBitrate
//
//        val default = bitrate.times(1000)
//
//        val toCheck = listOf(Pair("default", default))
//        toCheck.forEach {
//            if (it.second > guildMax)
//                ctx.replyHandler.replyEmbed(
//                    Embeds.error(
//                        "You provided a ${it.first} bitrate amount which is too high." +
//                        "\nYour server supports a max bitrate of ${guildMax / 1000} kbps, provide an amount within that value."
//                    )
//                )
//        }
//
//        ctx.guildData.generators[ctx.generatorIndex].waitingBitrate = default
//        guildDao.save(ctx.guildData)
//
//        ctx.replyHandler.replyEmbed(
//            Embeds.success(
//                "The bitrate for waiting vcs now is ${default / 1000} kbps"
//            )
//        )
//    }
//
//    @SubCommand(
//        name = "category",
//        description = "Set the category where waiting vcs get generated"
//    )
//    suspend fun category(
//        event: SlashCommandInteractionEvent,
//        ctx: GeneratorSettingsInteractionContext,
//        @CommandOption(
//            description = "The category for waiting vcs",
//            type = OptionType.CHANNEL,
//            channelTypes = [ChannelType.CATEGORY]
//        )
//        category: GuildChannel?
//    ) {
//        ctx.guildData.generators[ctx.generatorIndex].waitingCategory = category?.id
//        guildDao.save(ctx.guildData)
//
//        ctx.replyHandler.replyEmbed(
//            Embeds.success(
//                if (category != null)
//                    "Waiting vcs will now be generated under the category ${category.asMention}."
//                else
//                    "Waiting vcs will be generated at the top of the server since you didn't provide a category."
//            )
//        )
//    }
//
//    @SubCommand(
//        name = "creation",
//        description = "Choose whether Astro should create waiting vcs automatically"
//    )
//    suspend fun creation(
//        event: SlashCommandInteractionEvent,
//        ctx: GeneratorSettingsInteractionContext,
//        @CommandOption(
//            description = "Whether Astro should create waiting vcs automatically",
//            type = OptionType.BOOLEAN
//        )
//        create: Boolean
//    ) {
//        if (create && !premiumRequirementDetector.canCreateWaitingRoomOnVCGeneration(ctx.guildData)) {
//            ctx.replyHandler.replyPremiumRequired()
//            return
//        }
//
//        ctx.guildData.generators[ctx.generatorIndex].autoWaiting = create
//        guildDao.save(ctx.guildData)
//
//        ctx.replyHandler.replyEmbed(
//            Embeds.success(
//                if (create)
//                    "Astro will create waiting vcs for temporary voice channels."
//                else
//                    "Waiting vcs will not be created automatically."
//            )
//        )
//    }
//
//    @SubCommand(
//        name = "limit",
//        description = "Set the default user limit for waiting vcs"
//    )
//    suspend fun limit(
//        event: SlashCommandInteractionEvent,
//        ctx: GeneratorSettingsInteractionContext,
//        @CommandOption(
//            description = "The user limit for waiting vcs",
//            type = OptionType.INTEGER,
//            minValue = 0,
//            maxValue = 99
//        )
//        limit: Int
//    ) {
//        ctx.guildData.generators[ctx.generatorIndex].waitingUserLimit = limit
//        guildDao.save(ctx.guildData)
//
//        ctx.replyHandler.replyEmbed(
//            Embeds.success(
//                "The user limit for waiting vcs now is $limit users"
//            )
//        )
//    }
//
//    @SubCommand(
//        name = "name",
//        description = "The name for waiting vcs (see docs for variables)"
//    )
//    suspend fun name(
//        event: SlashCommandInteractionEvent,
//        ctx: GeneratorSettingsInteractionContext,
//        @CommandOption(
//            description = "The name for waiting vcs (see docs for variables)",
//            type = OptionType.STRING,
//            minLength = 2,
//            maxLength = 200
//        )
//        name: String
//    ) {
//        ctx.guildData.generators[ctx.generatorIndex].defaultWaitingName = name
//        guildDao.save(ctx.guildData)
//
//        ctx.replyHandler.replyEmbedAndComponent(
//            embed = Embeds.success(
//                "Waiting vcs will now have the following name:" +
//                        "\n> *$name*" +
//                        "\n\nYou can use ${Emojis.variables.formatted} variables to customize the name." +
//                        "\nSee the available variables with the button below"
//            ),
//            component = Buttons.Help.variables
//        )
//    }
//
//    @SubCommand(
//        name = "permissions",
//        description = "Set from which channel waiting vcs should inherit the permissions"
//    )
//    suspend fun permissions(
//        event: SlashCommandInteractionEvent,
//        ctx: GeneratorSettingsInteractionContext,
//        @CommandOption(
//            name = "inherit-from",
//            description = "The channel from which waiting vcs will inherit the permissions",
//            type = OptionType.STRING,
//        )
//        inheritFrom: PermissionsInherited,
//    ) {
//        ctx.guildData.generators[ctx.generatorIndex].waitingPermissionsInherited = inheritFrom
//        guildDao.save(ctx.guildData)
//
//        ctx.replyHandler.replyEmbed(
//            Embeds.success(
//                if (inheritFrom == PermissionsInherited.NONE)
//                    "Waiting vcs will not inherit any permissions"
//                else
//                    "Waiting vcs will inherit permissions from the ${inheritFrom.name.lowercase()}"
//            )
//        )
//    }
//
//    @SubCommand(
//        name = "position",
//        description = "Set the position where waiting vcs will be created relative to their temporary vc"
//    )
//    suspend fun position(
//        event: SlashCommandInteractionEvent,
//        ctx: GeneratorSettingsInteractionContext,
//        @CommandOption(
//            description = "The relative position for waiting vcs",
//            type = OptionType.STRING,
//            stringChoices = ["Before", "After", "Bottom"]
//        )
//        position: String
//    ) {
//        val initialPosition = InitialPosition.valueOf(position.uppercase())
//
//        ctx.guildData.generators[ctx.generatorIndex].waitingPosition = initialPosition
//        guildDao.save(ctx.guildData)
//
//        val readablePosition = when (initialPosition) {
//            InitialPosition.BEFORE -> "before their temporary vc"
//            InitialPosition.AFTER -> "after their temporary vc"
//            InitialPosition.BOTTOM -> "at the bottom of the category"
//        }
//
//        ctx.replyHandler.replyEmbed(
//            Embeds.success(
//                "All waiting vcs will be generated $readablePosition."
//            )
//        )
//    }
//}