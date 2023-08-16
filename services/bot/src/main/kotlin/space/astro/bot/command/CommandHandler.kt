package space.astro.bot.command

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend

private val log = KotlinLogging.logger {}

@Component
class CommandHandler(
    commands: List<ICommand>,
    val shardManager: ShardManager,
    val discordApplicationConfig: DiscordApplicationConfig,
    val applicationEventPublisher: ApplicationEventPublisher,
    val objectMapper: ObjectMapper,
    val statsClient: StatsClient
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

    @DelicateCoroutinesApi
    @EventListener
    fun receiveSlashCommand(event: SlashCommandInteractionEvent) {
        val guild = event.guild
//        if (guild == null) {
//            log.warn("Received slash command event without guild")
//            event.reply("This bot is only usable within a server.").queue()
//            return
//        }
        val member = event.member
        val isFromGuild = guild != null
        if (isFromGuild && member == null) {
            log.warn("Received slash command event from guild ${guild!!.id} without member")
            return
        }

        val channel = event.channel

        if (isFromGuild && discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
            !discordApplicationConfig.whitelistedGuilds.contains(guild!!.idLong)
        ) {
            log.warn(
                "Received slash command event outside of whitelisted guilds - guild id: {}",
                guild.id
            )
            event.reply("This command is not available outside of whitelisted guilds.")
                .setEphemeral(true).queue()
            return
        }

        val key = getFullKeyFromEvent(event)

        val commandContainer = commandsMap[event.name]
            ?: throw IllegalArgumentException("Couldn't find command container with name ${event.name}!")

        val (command, options) = commandContainer.commands[key]
            ?: throw IllegalArgumentException("Couldn't find matching function name for $key!")

        val optionArgs = Array(options.size) { index ->
            val type = command.parameters[3 + index].type.classifier as KClass<*>
            val name = options[index]
            when (type) {
                String::class -> event.getOption(name)?.asString
                Long::class -> event.getOption(name)?.asLong
                Int::class -> event.getOption(name)?.asLong?.toInt()
                Boolean::class -> event.getOption(name)?.asBoolean
                User::class -> event.getOption(name)?.asUser
                GuildChannel::class -> event.getOption(name)?.asChannel
                Role::class -> event.getOption(name)?.asRole
                else -> throw UnsupportedOperationException("Unable to handle option $name!")
            }
        }

        val commandContext = CommandContext(this, guild, member, event.user, channel)

        fireCommandAnalyticsEvent(key, event, guild?.idLong ?: 0L, channel.idLong, event.options)

        GlobalScope.launch {
            command.callSuspend(commandContainer, event, commandContext, *optionArgs)

            statsClient.submit(
                AbstractMeasurement(
                    "command_executions",
                    mapOf(
                        "count" to 1
                    ),
                    mapOf(
                        "name" to command.name,
                        "result" to CommandExecutionResult.SUCCEEDED.toString(),
                        "type" to CommandExecutionType.SLASH.toString(),
                    )
                )
            )
        }
    }

    private fun fireCommandAnalyticsEvent(
        key: String,
        event: SlashCommandInteractionEvent,
        guildId: Long,
        channelId: Long,
        options: MutableList<OptionMapping>
    ) {
        val optionsPairs = options.map {
            OptionPair(it.name, it.asString)
        }

        val analyticsEventMetaData = SlashCommandInvocationOptionsMetaData(optionsPairs)

        val analyticsEvent = AnalyticsEvent(
            listOf(AnalyticsEventReceiver.BIGQUERY),
            AnalyticsEventType.SLASH_COMMAND_INVOCATION,
            SlashCommandInvocationEventData(
                key,
                guildId,
                channelId,
                event.user.idLong,
                if (optionsPairs.isNotEmpty()) optionsPairs[0].name else null,
                if (optionsPairs.isNotEmpty()) optionsPairs[0].value else null,
                serialize(analyticsEventMetaData),
                LocalDateTime.now(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).toString(),
            )
        )

        applicationEventPublisher.publishEvent(analyticsEvent)
    }

    fun getFullKeyFromEvent(event: SlashCommandInteractionEvent): String {
        val isSubCommand = event.subcommandName != null
        val hasSubCommandGroup = event.subcommandGroup != null

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
