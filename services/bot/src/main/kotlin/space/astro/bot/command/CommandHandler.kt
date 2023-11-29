package space.astro.bot.command

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.core.ui.Embeds
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

private val log = KotlinLogging.logger {}

@Component
class CommandHandler(
    commands: List<ICommand>,
    val shardManager: ShardManager,
    val discordApplicationConfig: DiscordApplicationConfig,
    val applicationEventPublisher: ApplicationEventPublisher,
    val objectMapper: ObjectMapper,
    val temporaryVCDao: TemporaryVCDao,
    val guildDao: GuildDao,
    val configurationErrorEventPublisher: ConfigurationErrorEventPublisher
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

        if (guild == null) {
            log.warn("Received slash command event without guild")
            event.replyEmbeds(Embeds.error("This bot cannot be used outside servers!")).queue()
            return
        }

        val member = event.member
        if (member == null) {
            log.warn("Received slash command event from guild ${guild!!.id} without member")
            return
        }

        val channel = event.channel

        if (discordApplicationConfig.whitelistedGuilds.isNotEmpty() &&
            !discordApplicationConfig.whitelistedGuilds.contains(guild.idLong)
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
            val type = command.parameters[2 + index].type.classifier as KClass<*>
            val name = options[index]
            when (type) {
                String::class -> event.getOption(name)?.asString
                Long::class -> event.getOption(name)?.asLong
                Int::class -> event.getOption(name)?.asLong?.toInt()
                Boolean::class -> event.getOption(name)?.asBoolean
                User::class -> event.getOption(name)?.asUser
                Member::class -> event.getOption(name)?.asMember
                GuildChannel::class -> event.getOption(name)?.asChannel
                Role::class -> event.getOption(name)?.asRole
                else -> throw UnsupportedOperationException("Unable to handle option $name!")
            }
        }

        val commandContextBase = CommandContext(
            commandHandler = this,
            guild = guild,
            member = member,
            user = event.user,
            channel = channel
        )

        val commandContextParameter = command.parameters[1]

        val commandContext = when (val commandContextArgType = commandContextParameter.type.classifier as KClass<*>) {
            CommandContext::class -> commandContextBase
            VcCommandContext::class -> {
                val vcCommandContextInfo = commandContextParameter.findAnnotation<VcCommandContextInfo>()
                    ?: throw IllegalArgumentException("Found VcCommandContext parameter in command $key without VcCommandContextInfo annotation!")

                val vc = member.voiceState!!
                    .channel
                    ?.takeIf { it.type == ChannelType.VOICE }
                    ?.asVoiceChannel()
                    ?: throw IllegalArgumentException("Member is required to be in a VC for $key because the command requires a VcCommandContext, but the member isn't in a voice channel!")

                val temporaryVCsData = temporaryVCDao.getAll(guild.id)
                val temporaryVCData = temporaryVCsData.firstOrNull { it.id == vc.id }
                    ?: run {
                        event.replyEmbeds(Embeds.error("You must be in a temporary VC to use this command!"))
                            .setEphemeral(true).queue()
                        return
                    }

                if (vcCommandContextInfo.ownershipRequired) {
                    if (temporaryVCData.ownerId != member.id) {
                        event.replyEmbeds(Embeds.error("You need to be the owner of the temporary VC to use this command!"))
                            .setEphemeral(true).queue()
                    }
                }

                val guildData = guildDao.get(guild.id)
                    ?: run {
                        event.replyEmbeds(Embeds.error("Astro is not configured in this server!"))
                            .setEphemeral(true).queue()
                        return
                    }

                val generatorData = guildData.generators
                    .firstOrNull { it.id == temporaryVCData.generatorId }

                val generator = generatorData?.id?.let { guild.getVoiceChannelById(it) }

                if (generatorData == null || generator == null) {
                    event.replyEmbeds(Embeds.error("The generator of this temporary VC has been deleted!"))
                        .queue()
                    return
                }

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
                    vcOperationOrigin = vcCommandContextInfo.vcOperationOrigin
                )

                VcCommandContext(
                    vcOperationCTX = vcOperationCTX,
                    commandHandler = this,
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
                command.callSuspend(commandContainer, event, commandContext, *optionArgs)

                TODO("BIGQUERY")
            } catch (e: Exception) {
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
}
