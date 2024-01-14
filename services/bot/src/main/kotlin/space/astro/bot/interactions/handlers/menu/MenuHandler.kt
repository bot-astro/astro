package space.astro.bot.interactions.handlers.menu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.CooldownsManager
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
import space.astro.shared.core.util.extention.asRelativeTimestampFromNow
import space.astro.shared.core.util.ui.Links
import kotlin.reflect.full.callSuspend

private val log = KotlinLogging.logger {  }

@Component
class MenuHandler(
    menus: List<IMenu>,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    private val interactionContextBuilder: InteractionContextBuilder,
    private val guildDao: GuildDao,
    private val cooldownsManager: CooldownsManager,
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val coroutineScope: CoroutineScope,
    private val shardManager: ShardManager
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

    @EventListener
    fun receiveMenu(event: GenericSelectMenuInteractionEvent<*, *>) {
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
                log.warn("Received menu event from guild ${guild.id} without member")
                return@launch
            }

            if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
                !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
            ) {
                log.warn("Received menu event outside of whitelisted guilds - guild id: ${guild.id}")
                event.hook.editOriginalEmbeds(Embeds.error("This command is not available outside of whitelisted guilds."))
                    .setComponents()
                    .queue()
                return@launch
            }

            val keyParts = event.componentId.split("?")
            val key = keyParts.first()
            val menuContainer = menuMap[key]
                ?: throw IllegalArgumentException("Couldn't find menu container with id ${key}!")
            val menuRunnable = menuContainer.runnable
                ?: throw IllegalArgumentException("Couldn't find menu runnable with id ${key}!")

            ////////////////
            /// COOLDOWN ///
            ////////////////
            val cooldown = cooldownsManager.getUserActionCooldown(member.id, menuContainer.action)
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

            if (menuContainer.action.premium && (guildData == null || !premiumRequirementDetector.isGuildPremium(
                    guildData
                ))
            ) {
                event.replyWithPremiumRequired().queue()
                return@launch
            }

            ///////////////////////
            /// BOT PERMISSIONS ///
            ///////////////////////
            val botPermissions = menuContainer.action.botPermissions

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
            val originatedFromInterface = keyParts.lastOrNull()?.contains("interface=true") ?: false
            val interactionContextBase = InteractionContext(
                guild = guild,
                member = member,
                replyHandler = InteractionReplyHandler(
                    originatedFromInterface = originatedFromInterface,
                    originatedFromExistingMessage = true,
                    replyCallback = event,
                    modalCallback = event,
                    premiumReplyCallback = event,
                    shardManager = shardManager
                )
            )

            val interactionContextParameter = menuRunnable.parameters[2]

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
                menuRunnable.callSuspend(menuContainer, event, interactionContext)
            } catch (e: Exception) {
                when (e) {
                    is ConfigurationException -> {
                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            guildId = guild.id,
                            configurationErrorData = e.configurationErrorData
                        )

                        interactionContext.replyHandler.replyEmbed(Embeds.error("An error occurred because of an invalid configuration:\n> ${e.configurationErrorData.description}"))
                    }

                    is InsufficientPermissionException -> {
                        val configurationError = e.toConfigurationErrorDto()

                        configurationErrorEventPublisher.publishConfigurationErrorEvent(
                            guildId = guild.id,
                            configurationErrorData = configurationError
                        )

                        interactionContext.replyHandler.replyEmbed(Embeds.error("An error occurred because of missing permissions:\n> ${configurationError.description}"))
                    }

                    else -> {
                        interactionContext.replyHandler.replyEmbed(Embeds.error("An unknown error occurred, the developers are aware of it and will investigate it.\nIf you need support join the [support server](${Links.SUPPORT_SERVER})."))
                        throw e
                    }
                }
            }
        }
    }
}