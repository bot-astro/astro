package space.astro.bot.interactions.menu

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.core.ui.Embeds
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.interactions.InteractionContext
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

private val log = KotlinLogging.logger {  }

@Component
class MenuHandler(
    menus: List<IMenu>,
    val discordApplicationConfig: DiscordApplicationConfig,
    val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    val temporaryVCDao: TemporaryVCDao,
    val guildDao: GuildDao
) {
    val menuMap = HashMap<String, IMenu>()

    init {
        menus.forEach { menu ->
            val key = menu.id
            if (menuMap.containsKey(key)) {
                throw IllegalStateException("Found duplicate menu id: $key")
            } else {
                menuMap[key] = menu
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @EventListener
    fun receiveMenu(event: GenericSelectMenuInteractionEvent<*, *>) {
        val guild = event.guild

        if (guild == null) {
            log.warn("Received menu event without guild")
            event.hook.editOriginalEmbeds(Embeds.error("This bot cannot be used outside servers!"))
                .setComponents()
                .queue()
            return
        }

        val member = event.member
        if (member == null) {
            log.warn("Received menu event from guild ${guild.id} without member")
            return
        }

        val channel = event.channel

        if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
            !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
        ) {
            log.warn("Received menu event outside of whitelisted guilds - guild id: ${guild.id}")
            event.hook.editOriginalEmbeds(Embeds.error("This command is not available outside of whitelisted guilds."))
                .setComponents()
                .queue()
            return
        }

        val key = event.componentId
        val menuContainer = menuMap[key]
            ?: throw IllegalArgumentException("Couldn't find menu container with id ${key}!")
        val menuRunnable = menuContainer.runnable
            ?: throw IllegalArgumentException("Couldn't find menu runnable with id ${key}!")

        val interactionContextBase = InteractionContext(
            guild = guild,
            member = member,
            user = event.user,
            channel = channel
        )

        val interactionContextParameter = menuRunnable.parameters[2]

        val interactionContext =
            when (val commandContextArgType = interactionContextParameter.type.classifier as KClass<*>) {
                InteractionContext::class -> interactionContextBase
                VcInteractionContext::class -> {
                    val vcInteractionContextInfo = interactionContextParameter.findAnnotation<VcInteractionContextInfo>()
                        ?: throw IllegalArgumentException("Found VcCommandContext parameter in menu $key without VcCommandContextInfo annotation!")

                    val vc = member.voiceState!!
                        .channel
                        ?.takeIf { it.type == ChannelType.VOICE }
                        ?.asVoiceChannel()
                        ?: run {
                            event.hook.editOriginalEmbeds(Embeds.error("You need to be in a VC to use this command!"))
                                .setComponents()
                                .queue()

                            return
                        }

                    val temporaryVCsData = temporaryVCDao.getAll(guild.id)
                    val temporaryVCData = temporaryVCsData.firstOrNull { it.id == vc.id }
                        ?: run {
                            event.hook.editOriginalEmbeds(Embeds.error("You must be in a temporary VC to use this menu!"))
                                .setComponents()
                                .queue()
                            return
                        }

                    if (vcInteractionContextInfo.ownershipRequired) {
                        if (temporaryVCData.ownerId != member.id) {
                            event.hook.editOriginalEmbeds(Embeds.error("You need to be the owner of the temporary VC to use this menu!"))
                                .setComponents()
                                .queue()
                        }
                    }

                    val guildData = guildDao.get(guild.id)
                        ?: run {
                            event.hook.editOriginalEmbeds(Embeds.error("Astro is not configured in this server!"))
                                .setComponents()
                                .queue()
                            return
                        }

                    val generatorData = guildData.generators
                        .firstOrNull { it.id == temporaryVCData.generatorId }

                    val generator = generatorData?.id?.let { guild.getVoiceChannelById(it) }

                    if (generatorData == null || generator == null) {
                        event.hook.editOriginalEmbeds(Embeds.error("The generator of this temporary VC has been deleted!"))
                            .setComponents()
                            .queue()
                        return
                    }

                    @Suppress("DUPLICATE")
                    val privateChat = temporaryVCData.chatID?.let { guild.getTextChannelById(it) }
                    val waitingRoom = temporaryVCData.waitingID?.let { guild.getVoiceChannelById(it) }

                    val vcOperationCTX = VCOperationCTX(
                        guildData = guildData,
                        generator = generator,
                        generatorData = generatorData,
                        temporaryVCOwner = member,
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

                    VcInteractionContext(
                        vcOperationCTX = vcOperationCTX,
                        guild = guild,
                        member = member,
                        user = event.user,
                        channel = channel
                    )
                }

                else -> throw IllegalArgumentException("Command context of type $commandContextArgType is not recognized")
            }

        GlobalScope.launch {
            try {
                menuRunnable.callSuspend(menuContainer, event, interactionContext)
            } catch (e: Exception) {
                // TODO: reply
                when (e) {
                    is ConfigurationException -> configurationErrorEventPublisher.publishConfigurationErrorEvent(
                        guildId = guild.id,
                        configurationErrorDto = e.configurationErrorDto
                    )

                    is InsufficientPermissionException -> configurationErrorEventPublisher.publishConfigurationErrorEvent(
                        guildId = guild.id,
                        configurationErrorDto = e.toConfigurationErrorDto()
                    )

                    else -> throw e
                }
            }
        }
    }
}