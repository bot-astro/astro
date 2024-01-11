package space.astro.bot.interactions.button

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
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
class ButtonHandler(
    buttons: List<IButton>,
    val discordApplicationConfig: DiscordApplicationConfig,
    val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    val temporaryVCDao: TemporaryVCDao,
    val interactionContextBuilder: InteractionContextBuilder
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

        val channel = event.channel

        if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
            !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
        ) {
            log.warn("Received button event outside of whitelisted guilds - guild id: ${guild.id}")
            event.hook.editOriginalEmbeds(Embeds.error("This command is not available outside of whitelisted guilds."))
                .setComponents()
                .queue()
            return
        }

        val key = event.componentId
        // TODO: Add support for old interface button ids
        val buttonContainer = buttonMap[key]
            ?: throw IllegalArgumentException("Couldn't find button container with id ${key}!")
        val buttonRunnable = buttonContainer.runnable
            ?: throw IllegalArgumentException("Couldn't find button runnable with id ${key}!")

        val interactionContextBase = InteractionContext(
            guild = guild,
            member = member,
            user = event.user
        )

        val interactionContextParameter = buttonRunnable.parameters[2]

        val interactionContext =
            when (val commandContextArgType = interactionContextParameter.type.classifier as KClass<*>) {
                InteractionContext::class -> interactionContextBase
                VcInteractionContext::class -> {
                    val vcInteractionContextInfo = interactionContextParameter.findAnnotation<VcInteractionContextInfo>()
                        ?: throw IllegalArgumentException("Found VcCommandContext parameter in button $key without VcCommandContextInfo annotation!")

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
                buttonRunnable.callSuspend(buttonContainer, event, interactionContext)
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