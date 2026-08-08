package space.astro.bot.interactions.handlers.command.impl.predashboard
//
//import dev.minn.jda.ktx.coroutines.await
//import net.dv8tion.jda.api.Permission
//import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
//import net.dv8tion.jda.api.exceptions.ErrorResponseException
//import net.dv8tion.jda.api.requests.ErrorResponse
//import space.astro.bot.components.managers.InterfaceManager
//import space.astro.shared.core.components.managers.PremiumRequirementDetector
//import space.astro.bot.core.ui.Buttons
//import space.astro.bot.core.ui.Embeds
//import space.astro.bot.core.ui.Emojis
//import space.astro.bot.interactions.InteractionAction
//import space.astro.bot.interactions.context.SettingsInteractionContext
//import space.astro.bot.interactions.handlers.command.AbstractCommand
//import space.astro.bot.interactions.handlers.command.BaseCommand
//import space.astro.bot.interactions.handlers.command.Command
//import space.astro.bot.interactions.handlers.command.CommandCategory
//import space.astro.shared.core.daos.GuildDao
//import space.astro.shared.core.models.database.GeneratorData
//
//@Command(
//    name = "setup",
//    description = "Create a Generator, a connection and a error-log channel",
//    requiredPermissions = [Permission.MANAGE_CHANNEL],
//    category = CommandCategory.SETTINGS,
//    action = InteractionAction.SETTINGS
//)
//class SetupCommand(
//    private val premiumRequirementDetector: PremiumRequirementDetector,
//    private val interfaceManager: InterfaceManager,
//    private val guildDao: GuildDao
//) : AbstractCommand() {
//    @BaseCommand
//    suspend fun run(
//        event: SlashCommandInteractionEvent,
//        ctx: SettingsInteractionContext
//    ) {
//        if (!premiumRequirementDetector.canCreateGenerator(ctx.guildData)) {
//            ctx.replyHandler.replyEmbedAndComponent(
//                embed = Embeds.error("There are already 3 Generators setup in this server.\nPremium is required to have more than 3 Generators." +
//                        "\nPossible solutions:" + "\n• Get ${Emojis.premium.formatted} Premium" +
//                        "\n• Delete an existing generator with `/generator delete`"
//                ),
//                component = Buttons.appDirectoryUltimate
//            )
//            return
//        }
//
//        if (!premiumRequirementDetector.canCreateInterface(ctx.guildData)) {
//            ctx.replyHandler.replyEmbedAndComponent(
//                embed = Embeds.error(
//                    "There is already 1 Interface setup in this server." +
//                            "\nPremium is required to have more than 1 Interface." +
//                            "\nPossible solutions:" +
//                            "\n• Get ${Emojis.premium.formatted}" +
//                            "\n• Delete an existing interface with `/interface delete`"),
//                component = Buttons.appDirectoryUltimate
//            )
//            return
//        }
//
//        ctx.replyHandler.deferReply()
//
//        val guild = ctx.guild
//        val guildData = ctx.guildData
//
//        try {
//            if (guild.channels.size >= 500) {
//                ctx.replyHandler.replyEmbed(
//                    Embeds.error(
//                        "This server has reached the maximum number of channels and Astro cannot start the setup." +
//                        "\nDelete at least **3** channels and then rerun this command."
//                    )
//                )
//                return
//            }
//
//            val category = guild.createCategory("Astro").await()
//
//            val interfaceC = guild.createTextChannel("interface")
//                .addMemberPermissionOverride(ctx.guild.selfMember.idLong, Permission.VIEW_CHANNEL.rawValue, 0)
//                .setParent(category)
//                .await()
//
//            val interfaceData = interfaceManager.createInterface(interfaceC)
//            guildData.interfaces.add(interfaceData)
//
//            val generatorC = guild.createVoiceChannel("➕ Generator")
//                .setParent(category).await()
//
//            guildData.generators.add(
//                GeneratorData(
//                    id = generatorC.id,
//                    category = category.id,
//                    chatCategory = category.id
//                )
//            )
//
//            guildDao.save(guildData)
//
//            ctx.replyHandler.replyEmbedAndComponents(
//                Embeds.success(
//                    "Astro has been setup in ${category.asMention}. Here is what has been created:" +
//                            "\n• A Generator that can be used to create temporary voice channels: ${generatorC.asMention}" +
//                            "\nYou can edit this generator with all the `/generator` commands." +
//                            "\n\n• An Interface that can be used to run Astro commands via buttons: ${interfaceC.asMention}" +
//                            "\nYou can edit this interface with all the `/interface` commands." +
//                            "\n\nYou can also assign temporary roles to users in voice channels with `/connection create`!" +
//                            "\nFor additional help use `/help`" +
//                            "\n\n*You can change the name, category, position and everything of this channels!*"
//                ),
//                components = Buttons.Bundles.helpAndLinks
//            )
//        } catch (e: ErrorResponseException) {
//            return when (e.errorResponse) {
//                ErrorResponse.MAX_ROLES_PER_GUILD -> {
//                    ctx.replyHandler.replyEmbed(
//                        Embeds.error(
//                            "This server has reached the maximum number of roles and Astro cannot start the setup." +
//                            "\nDelete a role and then rerun this command."
//                        )
//                    )
//                }
//
//                ErrorResponse.MAX_CHANNELS -> {
//                    ctx.replyHandler.replyEmbed(
//                        Embeds.error(
//                            "This server has reached the maximum number of channels and Astro cannot start the setup." +
//                            "\nDelete at least **4** channels and then rerun this command."
//                        )
//                    )
//                }
//                else -> throw e
//            }
//        }
//    }
//}