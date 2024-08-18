package space.astro.bot.interactions.handlers.command

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.CooldownsManager
import space.astro.shared.core.components.managers.PremiumRequirementDetector
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
import space.astro.shared.core.models.analytics.AnalyticsEvent
import space.astro.shared.core.models.analytics.AnalyticsEventReceiver
import space.astro.shared.core.models.analytics.AnalyticsEventType
import space.astro.shared.core.models.analytics.SlashCommandInvocationEventData
import space.astro.shared.core.models.analytics.meta.SlashCommandInvocationOptionsMetaData
import space.astro.shared.core.models.analytics.meta.structure.OptionPair
import space.astro.shared.core.models.influx.ConfigurationErrorData
import space.astro.shared.core.util.extention.asRelativeTimestampFromNow
import space.astro.shared.core.util.ui.Links
import java.lang.reflect.InvocationTargetException
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend

private val log = KotlinLogging.logger {}

@Component
class CommandHandler(
    commands: List<ICommand>,
    private val shardManager: ShardManager,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val objectMapper: ObjectMapper,
    private val interactionContextBuilder: InteractionContextBuilder,
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val guildDao: GuildDao,
    private val cooldownsManager: CooldownsManager,
    private val coroutineScope: CoroutineScope
) {

    val commandsMap = HashMap<String, ICommand>()

    init {
        commands.forEach { command ->
            val name = command.data.name
            if (commandsMap.containsKey(name)) {
                throw IllegalStateException("Found duplicated command name key: $name")
            } else {
                commandsMap[name] = command
            }
        }
    }

    fun registerCommands() {
        if (discordApplicationConfig.commandGuilds.isNotEmpty()) {
            for (commandGuild in discordApplicationConfig.commandGuilds) {
                val guild = shardManager.getGuildById(commandGuild)
                    ?: throw RuntimeException("Guild not found")
                guild.updateCommands()
                    .addCommands(commandsMap.values.map { it.data })
                    .queue({ success ->
                        for (command in success) {
                            commandsMap[command.name]?.id = command.idLong
                        }
                        log.info("Bulk overwrote ${success.size} commands for guild ${guild.id}!")
                    }, { failure ->
                        throw RuntimeException(
                            "Unable to bulk overwrite all commands for guild ${guild.id}!!",
                            failure
                        )
                    })
            }
        } else {
            shardManager.shards[0].updateCommands()
                .addCommands(commandsMap.values.map { it.data })
                .queue({ success ->
                    for (command in success) {
                        commandsMap[command.name]?.id = command.idLong
                    }
                    log.info("Bulk overwrote ${success.size} commands!")
                }, { failure ->
                    throw RuntimeException("Unable to bulk overwrite all commands!", failure)
                })
        }
    }

    @EventListener
    fun receiveSlashCommand(event: SlashCommandInteractionEvent) {
        coroutineScope.launch {
            val guild = event.guild

            if (guild == null) {
                log.warn("Received slash command event without guild")
                event.replyEmbeds(Embeds.error("This bot cannot be used outside servers!")).queue()
                return@launch
            }

            val member = event.member
            if (member == null) {
                log.warn("Received slash command event from guild ${guild.id} without member")
                return@launch
            }

            ////////////////////
            /// WHITELISTING ///
            ////////////////////
            if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
                !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
            ) {
                log.warn("Received slash command event outside of whitelisted guilds - guild id: ${guild.id}")
                event.reply("This command is not available outside of whitelisted guilds.")
                    .setEphemeral(true).queue()
                return@launch
            }


            /////////////////////////////
            /// RETRIEVE COMMAND DATA ///
            /////////////////////////////
            val key = getFullKeyFromEvent(event)

            val commandContainer = commandsMap[event.name]
                ?: throw IllegalArgumentException("Couldn't find command container with name ${event.name}!")

            val (command, options) = commandContainer.commands[key]
                ?: throw IllegalArgumentException("Couldn't find matching function name for $key!")

            val optionArgs = Array(options.size) { index ->
                val type = command.parameters[3 + index].type.classifier as KClass<*>
                val name = options[index]
                if (type.java.isEnum) {
                    val enumNameValue = event.getOption(name)?.asString?.uppercase()

                    if (enumNameValue == null) {
                        enumNameValue
                    } else {
                        val enumConstants = type.java.enumConstants
                        enumConstants?.firstOrNull {
                            (it as Enum<*>).name == enumNameValue
                        }
                    }
                } else when (type) {
                    String::class -> event.getOption(name)?.asString
                    Long::class -> event.getOption(name)?.asLong
                    Int::class -> event.getOption(name)?.asLong?.toInt()
                    Boolean::class -> event.getOption(name)?.asBoolean
                    User::class -> event.getOption(name)?.asUser
                    Member::class -> {
                        val option = event.getOption(name)

                        if (option != null) {
                            option.asMember
                                ?: run {
                                    event.replyEmbeds(Embeds.error("You must provide a user from this server for `$name`"))
                                        .setEphemeral(true).queue()

                                    return@launch
                                }
                        } else null
                    }
                    GuildChannel::class -> event.getOption(name)?.asChannel
                    Role::class -> event.getOption(name)?.asRole
                    else -> throw UnsupportedOperationException("Unable to handle option $name!")
                }
            }


            ////////////////
            /// COOLDOWN ///
            ////////////////
            val cooldown = cooldownsManager.getUserActionCooldown(member.id, commandContainer.action)
            if (cooldown > 0) {
                event.replyEmbeds(Embeds.error("This command is on cooldown, you will be able to use it again in ${cooldown.asRelativeTimestampFromNow()}"))
                    .setEphemeral(true)
                    .queue()

                return@launch
            }

            /////////////////////
            /// PREMIUM CHECK ///
            /////////////////////
            val guildData = guildDao.get(guild.id)

            if (commandContainer.action.premium && (guildData == null || !premiumRequirementDetector.isGuildPremium(guildData))) {
                event.replyWithPremiumRequired().queue()
                return@launch
            }

            ////////////////////////
            /// USER PERMISSIONS ///
            ////////////////////////
            val memberPermissions = commandContainer.action.memberPermissions
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
            val botPermissions = commandContainer.action.botPermissions

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
                    messageEditCallback = null,
                    modalCallback = event,
                    premiumReplyCallback = event,
                    shardManager = shardManager
                )
            )

            val interactionContextParameter = command.parameters[2]

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

            /////////////////
            /// ANALYTICS ///
            /////////////////
            trackCommandAnalyticsEvent(key, event, guild, event.options)

            /////////////////////////////////////
            /// RUN COMMAND AND HANDLE ERRORS ///
            /////////////////////////////////////
            try {
                command.callSuspend(commandContainer, event, interactionContext, *optionArgs)
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
                                        "\nOriginal exception: $e" +
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

    private fun trackCommandAnalyticsEvent(
        key: String,
        event: SlashCommandInteractionEvent,
        guild: Guild,
        options: MutableList<OptionMapping>
    ) {
        val optionsPairs = options.map {
            OptionPair(it.name, it.asString)
        }

        val analyticsEventMetaData = SlashCommandInvocationOptionsMetaData(optionsPairs)

        val analyticsEvent = AnalyticsEvent(
            receivers = listOf(AnalyticsEventReceiver.BIGQUERY),
            type = AnalyticsEventType.SLASH_COMMAND_INVOCATION,
            data = SlashCommandInvocationEventData(
                name = key,
                guildId = guild.idLong,
                channelId = event.channel.idLong,
                userId = event.user.idLong,
                mainOptionName = if (optionsPairs.isNotEmpty()) optionsPairs[0].name else null,
                mainOptionValue = if (optionsPairs.isNotEmpty()) optionsPairs[0].value else null,
                rawOptions = serialize(analyticsEventMetaData),
                timestamp = LocalDateTime.now(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).toString(),
            )
        )

        applicationEventPublisher.publishEvent(analyticsEvent)
    }


    fun getFullKeyFromEvent(event: SlashCommandInteractionEvent): String {
        val isSubCommand = event.subcommandName != null
        val hasSubCommandGroup = event.subcommandGroup != null

        @Suppress("KotlinConstantConditions")
        return if (isSubCommand && hasSubCommandGroup) {
            "${event.name}.${event.subcommandGroup}.${event.subcommandName}"
        } else if (isSubCommand && !hasSubCommandGroup) {
            "${event.name}.${event.subcommandName}"
        } else {
            event.name
        }
    }

    private fun serialize(metaData: SlashCommandInvocationOptionsMetaData): String? {
        if (metaData.options.isEmpty()) {
            return null
        }
        return objectMapper.writeValueAsString(metaData)
    }
}
