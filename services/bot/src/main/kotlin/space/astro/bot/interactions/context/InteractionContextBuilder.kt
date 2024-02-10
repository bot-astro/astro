package space.astro.bot.interactions.context

import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.messages.Embed
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.daos.UserDao
import space.astro.shared.core.models.database.GuildData
import space.astro.shared.core.util.ui.Colors
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

private val log = KotlinLogging.logger {  }

@Component
class InteractionContextBuilder(
    private val temporaryVCDao: TemporaryVCDao,
    private val shardManager: ShardManager,
    private val guildDao: GuildDao,
    private val userDao: UserDao
) {

    /**
     * Builds and validates the interaction context
     *
     * @param interactionContextParameter the actual context parameter as declared in the command / button / menu / modal function
     * @param interactionContextBase
     * @param guildData
     * @return [InteractionContext]
     * @throws InteractionContextBuilderException if something goes wrong
     */
    suspend fun buildInteractionContext(
        interactionContextParameter: KParameter,
        interactionContextBase: InteractionContext,
        guildData: GuildData?
    ) : InteractionContext {
        return when (val commandContextArgType = interactionContextParameter.type.classifier as KClass<*>) {
            InteractionContext::class -> interactionContextBase
            VcInteractionContext::class -> {
                val vcInteractionContextInfo = interactionContextParameter.findAnnotation<VcInteractionContextInfo>()
                    ?: throw IllegalArgumentException("Found VcCommandContext parameter without VcCommandContextInfo annotation!")

                if (guildData == null) {
                    throw InteractionContextBuilderException(Embeds.error("Astro is not configured in this server!"))
                }

                buildVcInteractionContext(
                    interactionContextBase = interactionContextBase,
                    vcInteractionContextInfo = vcInteractionContextInfo,
                    guildData = guildData
                )
            }

            SettingsInteractionContext::class -> {
                val finalGuildData = guildData
                    ?: GuildData(guildID = interactionContextBase.guildId).also { guildDao.save(it) }

                val userData = userDao.getOrCreate(interactionContextBase.memberId)

                SettingsInteractionContext(
                    guildData = finalGuildData,
                    userData = userData,
                    guild = interactionContextBase.guild,
                    member = interactionContextBase.member,
                    replyHandler = interactionContextBase.replyHandler
                )
            }

            ConnectionSettingsInteractionContext::class -> {
                val finalGuildData = guildData
                    ?: GuildData(guildID = interactionContextBase.guildId).also { guildDao.save(it) }

                buildConnectionSettingsInteractionContext(
                    interactionContextBase = interactionContextBase,
                    guildData = finalGuildData
                )
            }

            GeneratorSettingsInteractionContext::class -> {
                val finalGuildData = guildData
                    ?: GuildData(guildID = interactionContextBase.guildId).also { guildDao.save(it) }

                buildGeneratorSettingsInteractionContext(
                    interactionContextBase = interactionContextBase,
                    guildData = finalGuildData
                )
            }

            InterfaceSettingsInteractionContext::class -> {
                val finalGuildData = guildData
                    ?: GuildData(guildID = interactionContextBase.guildId).also { guildDao.save(it) }

                buildInterfaceSettingsInteractionContext(
                    interactionContextBase = interactionContextBase,
                    guildData = finalGuildData
                )
            }

            TemplateSettingsInteractionContext::class -> {
                val finalGuildData = guildData
                    ?: GuildData(guildID = interactionContextBase.guildId).also { guildDao.save(it) }

                buildTemplateSettingsInteractionContext(
                    interactionContextBase = interactionContextBase,
                    guildData = finalGuildData
                )
            }

            else -> throw IllegalArgumentException("Command context of type $commandContextArgType is not recognized")
        }
    }

    /**
     * @throws InteractionContextBuilderException if something fails
     */
    private fun buildVcInteractionContext(
        interactionContextBase: InteractionContext,
        vcInteractionContextInfo: VcInteractionContextInfo,
        guildData: GuildData
    ) : VcInteractionContext {
        val guild = interactionContextBase.guild
        val member = interactionContextBase.member

        val vc = member.voiceState!!
            .channel
            ?.takeIf { it.type == ChannelType.VOICE }
            ?.asVoiceChannel()
            ?: throw InteractionContextBuilderException(Embeds.error("You need to be in a VC to use this command!"))

        val temporaryVCsData = temporaryVCDao.getAll(guild.id)
        val temporaryVCData = temporaryVCsData.firstOrNull { it.id == vc.id }
            ?: throw InteractionContextBuilderException(Embeds.error("You must be in a temporary VC to use this button!"))

        if (vcInteractionContextInfo.ownershipRequired) {
            if (temporaryVCData.ownerId != member.id) {
                throw InteractionContextBuilderException(Embeds.error("You need to be the owner of the temporary VC to use this button!"))
            }
        }

        val generatorData = guildData.generators
            .firstOrNull { it.id == temporaryVCData.generatorId }

        val generator = generatorData?.id?.let { guild.getVoiceChannelById(it) }

        if (generatorData == null || generator == null) {
            throw InteractionContextBuilderException(Embeds.error("The generator of this temporary VC has been deleted!"))
        }

        val privateChat = temporaryVCData.chatID?.let { guild.getTextChannelById(it) }
        val waitingRoom = temporaryVCData.waitingID?.let { guild.getVoiceChannelById(it) }

        val vcOperationCTX = VCOperationCTX(
            guildData = guildData,
            generator = generator,
            generatorData = generatorData,
            temporaryVCOwner = guild.getMemberById(temporaryVCData.ownerId),
            temporaryVC = vc,
            temporaryVCManager = vc.manager,
            temporaryVCData = temporaryVCData,
            temporaryVCsData = temporaryVCsData,
            privateChat = privateChat,
            privateChatManager = privateChat?.manager,
            waitingRoom = waitingRoom,
            waitingRoomManager = waitingRoom?.manager,
            vcOperationOrigin = vcInteractionContextInfo.vcOperationOrigin
        )

        return VcInteractionContext(
            vcOperationCTX = vcOperationCTX,
            guild = guild,
            member = member,
            replyHandler = interactionContextBase.replyHandler
        )
    }

    /**
     * @throws InteractionContextBuilderException if something fails
     */
    private suspend fun buildConnectionSettingsInteractionContext(
        interactionContextBase: InteractionContext,
        guildData: GuildData,
    ) : ConnectionSettingsInteractionContext {
        val guild = interactionContextBase.guild
        val member = interactionContextBase.member

        val selectedIndex = promptUserToSelectEntityAndGetIndex(
            interactionContextBase = interactionContextBase,
            interactionContextEntitySelection = InteractionContextEntitySelection.CONNECTION,
            guildData = guildData
        )


        return ConnectionSettingsInteractionContext(
            connectionData = guildData.connections[selectedIndex],
            guildData = guildData,
            guild = guild,
            member = member,
            replyHandler = interactionContextBase.replyHandler
        )
    }

    /**
     * @throws InteractionContextBuilderException if something fails
     */
    private suspend fun buildGeneratorSettingsInteractionContext(
        interactionContextBase: InteractionContext,
        guildData: GuildData,
    ) : GeneratorSettingsInteractionContext {
        val guild = interactionContextBase.guild
        val member = interactionContextBase.member

        val selectedIndex = promptUserToSelectEntityAndGetIndex(
            interactionContextBase = interactionContextBase,
            interactionContextEntitySelection = InteractionContextEntitySelection.GENERATOR,
            guildData = guildData
        )


        return GeneratorSettingsInteractionContext(
            generatorData = guildData.generators[selectedIndex],
            guildData = guildData,
            guild = guild,
            member = member,
            replyHandler = interactionContextBase.replyHandler
        )
    }

    /**
     * @throws InteractionContextBuilderException if something fails
     */
    private suspend fun buildInterfaceSettingsInteractionContext(
        interactionContextBase: InteractionContext,
        guildData: GuildData,
    ) : InterfaceSettingsInteractionContext {
        val guild = interactionContextBase.guild
        val member = interactionContextBase.member

        val selectedIndex = promptUserToSelectEntityAndGetIndex(
            interactionContextBase = interactionContextBase,
            interactionContextEntitySelection = InteractionContextEntitySelection.INTERFACE,
            guildData = guildData
        )


        return InterfaceSettingsInteractionContext(
            interfaceData = guildData.interfaces[selectedIndex],
            guildData = guildData,
            guild = guild,
            member = member,
            replyHandler = interactionContextBase.replyHandler
        )
    }

    /**
     * @throws InteractionContextBuilderException if something fails
     */
    private suspend fun buildTemplateSettingsInteractionContext(
        interactionContextBase: InteractionContext,
        guildData: GuildData,
    ) : TemplateSettingsInteractionContext {
        val guild = interactionContextBase.guild
        val member = interactionContextBase.member

        val selectedIndex = promptUserToSelectEntityAndGetIndex(
            interactionContextBase = interactionContextBase,
            interactionContextEntitySelection = InteractionContextEntitySelection.TEMPLATE,
            guildData = guildData
        )


        return TemplateSettingsInteractionContext(
            templateData = guildData.templates[selectedIndex],
            guildData = guildData,
            guild = guild,
            member = member,
            replyHandler = interactionContextBase.replyHandler
        )
    }

    /**
     * Prompts the user to select an entity that matches [interactionContextEntitySelection]
     *
     * @return the index of the entity in the corresponding collection
     * @throws InteractionContextBuilderException if something goes wrong
     */
    private suspend fun promptUserToSelectEntityAndGetIndex(
        interactionContextBase: InteractionContext,
        interactionContextEntitySelection: InteractionContextEntitySelection,
        guildData: GuildData,
    ): Int {
        val guild = interactionContextBase.guild

        val collectionsToCheck = when (interactionContextEntitySelection) {
            InteractionContextEntitySelection.TEMPLATE -> guildData.templates
            InteractionContextEntitySelection.GENERATOR -> guildData.generators
            InteractionContextEntitySelection.CONNECTION -> guildData.connections
            InteractionContextEntitySelection.INTERFACE -> guildData.interfaces
        }

        ///////////////////
        /// EMPTY CHECK ///
        ///////////////////
        if (collectionsToCheck.isEmpty()) {
            throw InteractionContextBuilderException(Embeds.error(
                "This command needs to be applied to a ${interactionContextEntitySelection.entityName} but there are 0 ${interactionContextEntitySelection.entityName} in this server." +
                        "\nYou can create a new ${interactionContextEntitySelection.entityName} with the command `/${interactionContextEntitySelection.slashCommandPath}`."
            ))
        }

        var selectedEntityIndex = -1

        /////////////////////
        /// ONLY 1 ENTITY ///
        /////////////////////
        if (collectionsToCheck.size == 1) {
            selectedEntityIndex = 0
        } else {
            ////////////////////////
            /// SEND SELECT MENU ///
            ////////////////////////
            val entityName = interactionContextEntitySelection.entityName
            val emoji = interactionContextEntitySelection.emoji

            // Entity selection via selection menus
            val options = when (interactionContextEntitySelection) {
                InteractionContextEntitySelection.TEMPLATE -> {
                    guildData.templates.mapIndexed { index, templateData ->
                        val label = "${index + 1} - #${templateData.name}"

                        SelectOption.of(label, index.toString())
                            .withEmoji(emoji)
                    }
                }

                InteractionContextEntitySelection.GENERATOR -> {
                    guildData.generators.mapIndexed { index, generatorData ->
                        val genChannel = guild.getVoiceChannelById(generatorData.id)
                        val label = "${index + 1} - #${genChannel?.name ?: "deleted-channel"}"

                        SelectOption.of(label, index.toString())
                            .withDescription("From category: ${genChannel?.parentCategory?.name ?: "not in a category"}")
                            .withEmoji(emoji)
                    }
                }

                InteractionContextEntitySelection.CONNECTION -> {
                    guildData.connections.mapIndexed { index, connectionData ->
                        val connectedChannel = guild.getGuildChannelById(connectionData.id)
                        val connectedRole = guild.getRoleById(connectionData.roleID)

                        val label =
                            "${index + 1} - #${connectedChannel?.name ?: "deleted-channel"} : @${connectedRole?.name ?: "deleted-role"}"

                        SelectOption.of(label, index.toString())
                            .withEmoji(interactionContextEntitySelection.emoji)
                    }
                }

                InteractionContextEntitySelection.INTERFACE -> {
                    guildData.interfaces.mapIndexed { index, interfaceD ->
                        val interfaceChannel = guild.getTextChannelById(interfaceD.channelID)

                        val label =
                            "${index + 1} - #${interfaceChannel?.name ?: "deleted-channel"}"

                        SelectOption.of(label, index.toString())
                            .withEmoji(emoji)
                            .withDescription("From category: ${interfaceChannel?.parentCategory?.name ?: "not in a category"}")
                    }
                }
            }

            if (options.size > 25) {

                /////////////////////////////
                /// MORE THAN 25 ENTITIES ///
                /////////////////////////////
                var optionsBaseIndex = 0

                val cancelButton = Button.danger(InteractionIds.getRandom(), "Cancel")
                val previousButton = Button.of(ButtonStyle.SECONDARY, InteractionIds.getRandom(), "Previous")
                val nextButton = Button.of(ButtonStyle.SECONDARY, InteractionIds.getRandom(), "Next")

                val buttonIdsList = listOf(cancelButton.id, previousButton.id, nextButton.id)

                var menu = StringSelectMenu.create(InteractionIds.getRandom())
                    .addOptions(options.subList(0, 25))
                    .setPlaceholder("Choose a $entityName")
                    .build()

                interactionContextBase.replyHandler.reply(
                    embed = Embed(
                        color = Colors.purple.rgb,
                        title = "${emoji.formatted} $entityName selector",
                        description = "**Select a $entityName on which you want to apply this command.**" +
                                "\n\nTo select a $entityName use the menu under this message." +
                                "\nSince you have more than 25 $entityName you can use the `Previous` and `Next` buttons to navigate trough them."
                    ),
                    components = listOf(ActionRow.of(menu), ActionRow.of(cancelButton, previousButton, nextButton))
                )

                var stayInLoop = true

                while (stayInLoop) {
                    val event = withTimeoutOrNull(60000) {
                        shardManager.await<GenericComponentInteractionCreateEvent> {
                            (it is StringSelectInteractionEvent && it.componentId == menu.id)
                                    || (it is ButtonInteractionEvent && buttonIdsList.contains(it.componentId))
                        }
                    }

                    if (event == null) {
                        throw InteractionContextBuilderException(Embeds.error("You took too long to select a $entityName"))
                    }

                    interactionContextBase.replyHandler.setCallbacksFromComponentEvent(event)

                    if (event is ButtonInteractionEvent) {
                        if (event.componentId == cancelButton.id) {
                            throw InteractionContextBuilderException(Embeds.default("Command canceled"))
                        } else {
                            interactionContextBase.replyHandler.deferReply()
                            optionsBaseIndex = if (event.componentId == previousButton.id)
                                (optionsBaseIndex - 1).coerceAtLeast(0)
                            else
                                (optionsBaseIndex + 1).coerceAtMost((options.size - 1) / 25)

                            val maxIndex = ((optionsBaseIndex * 25) + 25).coerceAtMost(options.size)

                            menu = StringSelectMenu.create(InteractionIds.getRandom())
                                .addOptions(options.subList(optionsBaseIndex * 25, maxIndex))
                                .setPlaceholder("Choose a $entityName")
                                .build()

                            interactionContextBase.replyHandler.editComponents(
                                listOf(
                                    ActionRow.of(menu),
                                    ActionRow.of(cancelButton, previousButton, nextButton)
                                )
                            )
                        }
                    } else {
                        selectedEntityIndex = (event as StringSelectInteractionEvent).values.first().toInt()
                        stayInLoop = false
                    }
                }
            } else {

                /////////////////////////////
                /// LESS THAN 25 ENTITIES ///
                /////////////////////////////
                val menu = StringSelectMenu.create(InteractionIds.getRandom())
                    .addOptions(options)
                    .setPlaceholder("Choose a $entityName")
                    .build()

                val cancelButton = Button.danger(InteractionIds.getRandom(), "Cancel")

                interactionContextBase.replyHandler.reply(
                    embed = Embed(
                        color = Colors.purple.rgb,
                        title = "${emoji.formatted} $entityName selector",
                        description = "**Select a $entityName on which you want to apply this command.**" +
                                "\nTo select a $entityName use the menu under this message."
                    ),
                    components = listOf(ActionRow.of(menu), ActionRow.of(cancelButton))
                )

                val event = withTimeoutOrNull(60000) {
                    shardManager.await<GenericComponentInteractionCreateEvent> {
                        (it is StringSelectInteractionEvent && it.componentId == menu.id)
                                || (it is ButtonInteractionEvent && it.componentId == cancelButton.id)
                    }
                }

                if (event == null) {
                    throw InteractionContextBuilderException(Embeds.error("You took too long to select a $entityName"))
                }

                interactionContextBase.replyHandler.setCallbacksFromComponentEvent(event)

                if (event is ButtonInteractionEvent) {
                    throw InteractionContextBuilderException(Embeds.default("Command canceled"))
                }

                selectedEntityIndex = (event as StringSelectInteractionEvent).values.first().toInt()
            }
        }

        return selectedEntityIndex
    }
}