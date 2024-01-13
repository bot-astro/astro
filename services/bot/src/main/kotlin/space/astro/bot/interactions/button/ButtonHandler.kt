package space.astro.bot.interactions.button

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.CooldownsManager
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.core.ui.Embeds
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.interactions.*
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.util.extention.asRelativeTimestampFromNow
import space.astro.shared.core.util.ui.Links
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

private val log = KotlinLogging.logger {  }

@Component
class ButtonHandler(
    buttons: List<IButton>,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    private val interactionContextBuilder: InteractionContextBuilder,
    private val guildDao: GuildDao,
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val cooldownsManager: CooldownsManager,
    private val configurationErrorService: ConfigurationErrorService
) {
    val buttonMap = HashMap<String, IButton>()

    init {
        buttons.forEach { menu ->
            val key = menu.id
            if (buttonMap.containsKey(key)) {
                throw IllegalStateException("Found duplicate button id: $key")
            } else {
                buttonMap[key] = menu
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @EventListener
    fun receiveButton(event: ButtonInteractionEvent) {
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
            log.warn("Received button event from guild ${guild.id} without member")
            return
        }

        if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
            !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
        ) {
            log.warn("Received button event outside of whitelisted guilds - guild id: ${guild.id}")
            event.hook.editOriginalEmbeds(Embeds.error("This command is not available outside of whitelisted guilds."))
                .setComponents()
                .queue()
            return
        }

        val keyParts = event.componentId.split("?")
        var key = keyParts.first()
        val usedInterfaceComponent = keyParts.lastOrNull()?.contains("interface=true") ?: false


        if (key.startsWith("cmd>")) {
            key = mapOldIdToNewId(key.substring(4))
                ?: throw ConfigurationException(configurationErrorService.invalidOldInterface(event.channel.id))
        }

        val buttonContainer = buttonMap[key]
            ?: throw IllegalArgumentException("Couldn't find button container with id ${key}!")
        val buttonRunnable = buttonContainer.runnable
            ?: throw IllegalArgumentException("Couldn't find button runnable with id ${key}!")

        val interactionContextBase = InteractionContext(
            guild = guild,
            member = member,
            user = event.user
        )

        ////////////////
        /// COOLDOWN ///
        ////////////////
        val cooldown = cooldownsManager.getUserActionCooldown(member.id, buttonContainer.action)
        if (cooldown > 0) {
            event.replyEmbeds(Embeds.error("This action is on cooldown, you will be able to use it again in ${cooldown.asRelativeTimestampFromNow()}"))
                .setEphemeral(true)
                .queue()

            return
        }

        /////////////////////
        /// PREMIUM CHECK ///
        /////////////////////
        val guildData = guildDao.get(guild.id)

        if (buttonContainer.action.premium && (guildData == null || !premiumRequirementDetector.isGuildPremium(guildData))) {
            event.replyEmbeds(Embeds.error("Premium is required to use this button!"))
                .setEphemeral(true)
                .queue()

            return
        }

        val interactionContextParameter = buttonRunnable.parameters[2]

        val interactionContext =
            when (val commandContextArgType = interactionContextParameter.type.classifier as KClass<*>) {
                InteractionContext::class -> interactionContextBase
                VcInteractionContext::class -> {
                    val vcInteractionContextInfo = interactionContextParameter.findAnnotation<VcInteractionContextInfo>()
                        ?: throw IllegalArgumentException("Found VcCommandContext parameter in button $key without VcCommandContextInfo annotation!")

                    if (guildData == null) {
                        event.replyEmbeds(Embeds.error("Astro is not configured in this server!"))
                            .setEphemeral(true)
                            .queue()

                        return
                    }

                    try {
                        interactionContextBuilder.buildVcInteractionContext(
                            interactionCreateEvent = event,
                            vcInteractionContextInfo = vcInteractionContextInfo,
                            usedInterfaceComponent = usedInterfaceComponent,
                            guildData = guildData
                        )
                    } catch (e: InteractionContextBuilderException) {
                        event.replyEmbeds(e.errorEmbed)
                            .setEphemeral(true)
                            .queue()

                        return
                    }
                }

                else -> throw IllegalArgumentException("Command context of type $commandContextArgType is not recognized")
            }

        GlobalScope.launch {
            try {
                buttonRunnable.callSuspend(buttonContainer, event, interactionContext)
            } catch (e: Exception) {
                when (e) {
                    is ConfigurationException -> {
                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            guildId = guild.id,
                            configurationErrorDto = e.configurationErrorDto
                        )

                        event.replyEmbeds(Embeds.error("An error occurred because of an invalid configuration:\n> ${e.configurationErrorDto.description}"))
                            .setEphemeral(true)
                            .queue()
                    }

                    is InsufficientPermissionException -> {
                        val configurationError = e.toConfigurationErrorDto()

                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            guildId = guild.id,
                            configurationErrorDto = configurationError
                        )

                        event.replyEmbeds(Embeds.error("An error occurred because of missing permissions:\n> ${configurationError.description}"))
                    }

                    else -> {
                        event.replyEmbeds(Embeds.error("An unknown error occurred, the developers are aware of it and will investigate it.\nIf you need support join the [support server](${Links.SUPPORT_SERVER})."))
                            .setEphemeral(true)
                            .queue()

                        throw e
                    }
                }
            }
        }
    }

    fun mapOldIdToNewId(oldId: String): String? {
        return when(oldId) {
            "vc-name" -> InteractionIds.Button.VC_NAME
            "vc-limit" -> InteractionIds.Button.VC_LIMIT
            "vc-bitrate" -> InteractionIds.Button.VC_BITRATE
            "vc-region" -> InteractionIds.Button.VC_REGION
            "vc-template" -> InteractionIds.Button.VC_TEMPLATE

            "vc-unlock" -> InteractionIds.Button.VC_UNLOCK
            "vc-lock" -> InteractionIds.Button.VC_LOCK
            "vc-hide" -> InteractionIds.Button.VC_HIDE
            "vc-unhide" -> InteractionIds.Button.VC_UNHIDE
            "vc-ban" -> InteractionIds.Button.VC_BAN
            "vc-permit" -> InteractionIds.Button.VC_PERMIT
            "vc-invite" -> InteractionIds.Button.VC_INVITE
            "vc-reset" -> InteractionIds.Button.VC_RESET

            "vc-claim" -> InteractionIds.Button.VC_CLAIM
            "vc-transfer" -> InteractionIds.Button.VC_TRANSFER

            "vc-chat" -> InteractionIds.Button.VC_CHAT
            "vc-logs" -> InteractionIds.Button.VC_LOGS

            "vc-waiting" -> InteractionIds.Button.VC_WAITING_ROOM
            else -> null
        }
    }
}