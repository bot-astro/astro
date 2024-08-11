package space.astro.bot.interactions.handlers.button

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.CooldownsManager
import space.astro.shared.core.components.managers.PremiumRequirementDetector
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.core.ui.Embeds
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.context.InteractionContextBuilder
import space.astro.bot.interactions.context.InteractionContextBuilderException
import space.astro.bot.interactions.reply.InteractionReplyHandler
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.influx.ConfigurationErrorData
import space.astro.shared.core.util.extention.asRelativeTimestampFromNow
import space.astro.shared.core.util.ui.Links
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.callSuspend

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
    private val configurationErrorService: ConfigurationErrorService,
    private val coroutineScope: CoroutineScope,
    private val shardManager: ShardManager
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

    @EventListener
    fun receiveButton(event: ButtonInteractionEvent) {
        coroutineScope.launch {
            val guild = event.guild

            if (guild == null) {
                log.warn("Received menu event without guild")
                event.hook.editOriginalEmbeds(Embeds.error("This bot cannot be used outside servers!"))
                    .setComponents()
                    .queue()
                return@launch
            }

            val member = event.member
            if (member == null) {
                log.warn("Received button event from guild ${guild.id} without member")
                return@launch
            }

            if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
                !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
            ) {
                log.warn("Received button event outside of whitelisted guilds - guild id: ${guild.id}")
                event.hook.editOriginalEmbeds(Embeds.error("This command is not available outside of whitelisted guilds."))
                    .setComponents()
                    .queue()
                return@launch
            }

            val keyParts = event.componentId.split("?")
            var key = keyParts.first()
            var originatedFromOldInterface = false

            if (key.startsWith("cmd>")) {
                key = mapOldIdToNewId(key.substring(4))
                    ?: run {
                        val configErrorData = configurationErrorService.invalidOldInterface(guild.id, event.channel.id)

                        event.replyEmbeds(Embeds.error(configErrorData.description))
                            .setEphemeral(true)
                            .queue()

                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            configurationErrorData = configErrorData
                        )

                        return@launch
                    }

                originatedFromOldInterface = true
            }

            val buttonContainer = buttonMap[key]
                ?: run {
                    log.debug { "Couldn't find button container with id ${key}!" }
                    return@launch
                }
            val buttonRunnable = buttonContainer.runnable
                ?: run {
                    log.debug { "Couldn't find button runnable with id ${key}!" }
                    return@launch
                }

            ////////////////
            /// COOLDOWN ///
            ////////////////
            val cooldown = cooldownsManager.getUserActionCooldown(member.id, buttonContainer.action)
            if (cooldown > 0) {
                event.replyEmbeds(Embeds.error("This action is on cooldown, you will be able to use it again in ${cooldown.asRelativeTimestampFromNow()}"))
                    .setEphemeral(true)
                    .queue()

                return@launch
            }

            /////////////////////
            /// PREMIUM CHECK ///
            /////////////////////
            val guildData = guildDao.get(guild.id)

            if (buttonContainer.action.premium && (guildData == null || !premiumRequirementDetector.isGuildPremium(
                    guildData
                ))
            ) {
                event.replyWithPremiumRequired().queue()
                return@launch
            }

            ////////////////////////
            /// USER PERMISSIONS ///
            ////////////////////////
            val memberPermissions = buttonContainer.action.memberPermissions
            if (memberPermissions.isNotEmpty()) {
                if (!member.hasPermission(memberPermissions)) {
                    event.replyEmbeds(
                        Embeds.error(
                            "You need the following permissions to run this command: ${
                                memberPermissions.joinToString(
                                    ", "
                                ) { it.getName() }
                            }"
                        )
                    )
                        .setEphemeral(true)
                        .queue()

                    return@launch
                }
            }

            ///////////////////////
            /// BOT PERMISSIONS ///
            ///////////////////////
            val botPermissions = buttonContainer.action.botPermissions

            if (botPermissions.isNotEmpty()) {
                if (!guild.selfMember.hasPermission(botPermissions) && guildData?.allowMissingAdminPerm != true) {
                    event.replyEmbeds(
                        Embeds.error(
                            "Astro needs to following permissions to run this command: ${
                                botPermissions.joinToString(
                                    ", "
                                ) { it.getName() }
                            }"
                        )
                    )
                        .setEphemeral(true)
                        .queue()

                    return@launch
                }
            }

            /////////////////////////////////
            /// BUILD INTERACTION CONTEXT ///
            /////////////////////////////////
            val originatedFromInterface = originatedFromOldInterface || keyParts.lastOrNull()?.contains("interface=true") ?: false

            val interactionContextBase = InteractionContext(
                guild = guild,
                member = member,
                replyHandler = InteractionReplyHandler(
                    originatedFromInterface = originatedFromInterface,
                    originatedFromExistingMessage = true,
                    replyCallback = event,
                    messageEditCallback = event,
                    modalCallback = event,
                    premiumReplyCallback = event,
                    shardManager = shardManager
                )
            )

            val interactionContextParameter = buttonRunnable.parameters[2]

            val interactionContext = try {
                interactionContextBuilder.buildInteractionContext(
                    interactionContextParameter = interactionContextParameter,
                    interactionContextBase = interactionContextBase,
                    guildData = guildData
                )
            } catch (e: InteractionContextBuilderException) {
                interactionContextBase.replyHandler.replyEmbed(e.errorEmbed)
                return@launch
            }

            try {
                buttonRunnable.callSuspend(buttonContainer, event, interactionContext)
            } catch (e: Exception) {
                val exception = if (e is InvocationTargetException) e.cause ?: e.targetException else e

                when (exception) {
                    is ConfigurationException -> {
                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            configurationErrorData = exception.configurationErrorData
                        )

                        interactionContext.replyHandler.replyEmbed(Embeds.error("An error occurred because of an invalid configuration:\n\n${exception.configurationErrorData.description}"))
                    }

                    is InsufficientPermissionException -> {
                        val configurationError = exception.toConfigurationErrorDto(guild.id)

                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            configurationErrorData = configurationError
                        )

                        interactionContext.replyHandler.replyEmbed(Embeds.error("An error occurred because of missing permissions:\n> ${configurationError.description}"))
                    }

                    else -> {
                        val configurationError = ConfigurationErrorData(guild.id, exception.toString())

                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            configurationErrorData = configurationError
                        )

                        interactionContext.replyHandler.replyEmbed(
                            Embeds.error(
                                "An unknown error occurred, the developers are aware of it and will investigate it." +
                                        "\n\nError: ${e.message ?: "Unknown"}" +
                                        "\nFull exception: $e" +
                                        "\nParsed: $exception" +
                                        "\n\nIf you need support join the [support server](${Links.SUPPORT_SERVER})."
                            )
                        )
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