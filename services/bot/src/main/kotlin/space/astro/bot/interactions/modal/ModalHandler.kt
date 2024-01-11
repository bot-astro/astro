package space.astro.bot.interactions.modal

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.core.ui.Embeds
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.interactions.InteractionContext
import space.astro.bot.interactions.InteractionContextBuilder
import space.astro.bot.interactions.InteractionContextBuilderException
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.shared.core.daos.TemporaryVCDao
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

private val log = KotlinLogging.logger {  }

@Component
class ModalHandler(
    modals: List<IModal>,
    val discordApplicationConfig: DiscordApplicationConfig,
    val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    val temporaryVCDao: TemporaryVCDao,
    val interactionContextBuilder: InteractionContextBuilder
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

    @OptIn(DelicateCoroutinesApi::class)
    @EventListener
    fun receiveModal(event: ModalInteractionEvent) {
        val guild = event.guild

        if (guild == null) {
            log.warn("Received modal event without guild")
            event.hook.editOriginalEmbeds(Embeds.error("This bot cannot be used outside servers!"))
                .setComponents()
                .queue()
            return
        }

        val member = event.member
        if (member == null) {
            log.warn("Received modal event from guild ${guild.id} without member")
            return
        }

        val channel = event.channel

        if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
            !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
        ) {
            log.warn("Received modal event outside of whitelisted guilds - guild id: ${guild.id}")
            event.hook.editOriginalEmbeds(Embeds.error("This command is not available outside of whitelisted guilds."))
                .setComponents()
                .queue()
            return
        }

        val key = event.modalId
        val modalContainer = modalMap[key]
            ?: throw IllegalArgumentException("Couldn't find modal container with id ${key}!")
        val modalRunnable = modalContainer.runnable
            ?: throw IllegalArgumentException("Couldn't find modal runnable with id ${key}!")

        val interactionContextBase = InteractionContext(
            guild = guild,
            member = member,
            user = event.user
        )

        val interactionContextParameter = modalRunnable.parameters[2]

        val interactionContext =
            when (val commandContextArgType = interactionContextParameter.type.classifier as KClass<*>) {
                InteractionContext::class -> interactionContextBase
                VcInteractionContext::class -> {
                    val vcInteractionContextInfo = interactionContextParameter.findAnnotation<VcInteractionContextInfo>()
                        ?: throw IllegalArgumentException("Found VcCommandContext parameter in modal $key without VcCommandContextInfo annotation!")

                    try {
                        interactionContextBuilder.buildVcInteractionContext(
                            interactionCreateEvent = event,
                            vcInteractionContextInfo = vcInteractionContextInfo
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
                modalRunnable.callSuspend(modalContainer, event, interactionContext)
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