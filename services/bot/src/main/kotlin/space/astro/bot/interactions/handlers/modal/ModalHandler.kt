package space.astro.bot.interactions.handlers.modal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.core.ui.Embeds
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.context.InteractionContextBuilder
import space.astro.bot.interactions.context.InteractionContextBuilderException
import space.astro.bot.interactions.reply.InteractionReplyHandler
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.models.influx.ConfigurationErrorData
import space.astro.shared.core.util.ui.Links
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.callSuspend

private val log = KotlinLogging.logger {  }

@Component
class ModalHandler(
    modals: List<IModal>,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    private val interactionContextBuilder: InteractionContextBuilder,
    private val guildDao: GuildDao,
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val coroutineScope: CoroutineScope,
    private val shardManager: ShardManager
) {
    val modalMap = HashMap<String, IModal>()

    init {
        modals.forEach { menu ->
            val key = menu.id
            if (modalMap.containsKey(key)) {
                throw IllegalStateException("Found duplicate modal id: $key")
            } else {
                modalMap[key] = menu
            }
        }
    }

    @EventListener
    fun receiveModal(event: ModalInteractionEvent) {
        coroutineScope.launch {
            val guild = event.guild

            if (guild == null) {
                log.warn("Received modal event without guild")
                event.hook.editOriginalEmbeds(Embeds.error("This bot cannot be used outside servers!"))
                    .setComponents()
                    .queue()
                return@launch
            }

            val member = event.member
            if (member == null) {
                log.warn("Received modal event from guild ${guild.id} without member")
                return@launch
            }

            if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
                !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
            ) {
                log.warn("Received modal event outside of whitelisted guilds - guild id: ${guild.id}")
                event.hook.editOriginalEmbeds(Embeds.error("This command is not available outside of whitelisted guilds."))
                    .setComponents()
                    .queue()
                return@launch
            }

            val key = event.modalId
            val modalContainer = modalMap[key]
                ?: run {
                    log.debug { "Couldn't find modal container with id ${key}!" }
                    return@launch
                }
            val modalRunnable = modalContainer.runnable
                ?: run {
                    log.debug { "Couldn't find modal runnable with id ${key}!" }
                    return@launch
                }

            ///////////////////////////////////
            /// MODALS DON'T HAVE COOLDOWNS ///
            ///////////////////////////////////
//            val cooldown = cooldownsManager.getUserActionCooldown(member.id, modalContainer.action)
//            if (cooldown > 0) {
//                event.replyEmbeds(Embeds.error("This action is on cooldown, you will be able to use it again in ${cooldown.asRelativeTimestampFromNow()}"))
//                    .setEphemeral(true)
//                    .queue()
//
//                return@launch
//            }

            /////////////////////
            /// PREMIUM CHECK ///
            /////////////////////
            val guildData = guildDao.get(guild.id)

            if (modalContainer.action.premium && (guildData == null || !premiumRequirementDetector.isGuildPremium(
                    guildData
                ))
            ) {
                event.replyEmbeds(Embeds.error("Premium is required to use this modal!"))
                    .setEphemeral(true)
                    .queue()

                return@launch
            }

            ////////////////////////
            /// USER PERMISSIONS ///
            ////////////////////////
            val memberPermissions = modalContainer.action.memberPermissions
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
            val botPermissions = modalContainer.action.botPermissions

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
            val interactionContextBase = InteractionContext(
                guild = guild,
                member = member,
                replyHandler = InteractionReplyHandler(
                    originatedFromInterface = false,
                    originatedFromExistingMessage = false,
                    replyCallback = event,
                    messageEditCallback = event,
                    modalCallback = null,
                    premiumReplyCallback = null,
                    shardManager = shardManager
                )
            )

            val interactionContextParameter = modalRunnable.parameters[2]

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
                modalRunnable.callSuspend(modalContainer, event, interactionContext)
            } catch (e: Exception) {
                val exception = if (e is InvocationTargetException) e.targetException else e

                when (exception) {
                    is ConfigurationException -> {
                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            guildId = guild.id,
                            configurationErrorData = exception.configurationErrorData
                        )

                        interactionContext.replyHandler.replyEmbed(Embeds.error("An error occurred because of an invalid configuration:\n\n${exception.configurationErrorData.description}"))
                    }

                    is InsufficientPermissionException -> {
                        val configurationError = exception.toConfigurationErrorDto()

                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            guildId = guild.id,
                            configurationErrorData = configurationError
                        )

                        interactionContext.replyHandler.replyEmbed(Embeds.error("An error occurred because of missing permissions:\n> ${configurationError.description}"))
                    }

                    else -> {
                        val configurationError = ConfigurationErrorData(e.message ?: "Unknown issue, please contact developers!")

                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            guildId = guild.id,
                            configurationErrorData = configurationError
                        )

                        interactionContext.replyHandler.replyEmbed(
                            Embeds.error(
                                "An unknown error occurred, the developers are aware of it and will investigate it." +
                                        "\n\nError: ${e.message ?: "Unknown"}" +
                                        "\n\nIf you need support join the [support server](${Links.SUPPORT_SERVER})."
                            )
                        )
                        throw e
                    }
                }
            }
        }
    }
}