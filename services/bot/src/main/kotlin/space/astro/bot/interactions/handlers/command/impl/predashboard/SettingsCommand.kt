package space.astro.bot.interactions.handlers.command.impl.predashboard

import kotlinx.coroutines.delay
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.*
import space.astro.shared.core.daos.ConfigurationErrorDao
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao
import java.text.SimpleDateFormat
import java.util.*

@Command(
    name = "settings",
    description = "Edit Astro's settings in your server",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.HIGH_COOLDOWN_SETTINGS
)
class SettingsCommand(
    private val configurationErrorDao: ConfigurationErrorDao,
    private val temporaryVCDao: TemporaryVCDao,
    private val guildDao: GuildDao
) : AbstractCommand() {
    @SubCommand(
        name = "admin-permission",
        description = "Whether Astro should enforce Administrator permission or not"
    )
    suspend fun adminPermission(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext,
        @CommandOption(
            description = "True if Astro should require Administrator permissions, false otherwise",
            type = OptionType.BOOLEAN
        )
        required: Boolean
    ) {
        ctx.guildData.allowMissingAdminPerm = !required
        guildDao.save(ctx.guildData)

        ctx.replyHandler.replyEmbed(
            Embeds.default(
                if (required)
                    "Astro will now require Administrator permissions to work"
                else
                    "Astro will not require Administrator permissions to work." +
                            "\nYou can always see encountered errors using `/settings errors`"
            )
        )
    }


    @SubCommand(
        name = "errors",
        description = "Shows configuration errors detected by Astro"
    )
    suspend fun errors(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {
        ctx.replyHandler.deferReply()

        val errors = configurationErrorDao.get(ctx.guildId)

        if (errors.isEmpty()) {
            ctx.replyHandler.replyEmbed(Embeds.default("Astro didn't report any errors for this server"))
            return
        }

        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm")

        val description = "Here are the last detected errors by Astro:" +
                "\n\n${errors.joinToString("\n\n") { 
                    "> ${it.description}" + if (it.instant != null) "\n${simpleDateFormat.format(Date.from(it.instant!!))}" else ""
                }}"
                    .take(MessageEmbed.DESCRIPTION_MAX_LENGTH)

        ctx.replyHandler.replyEmbed(
            Embeds.default(description)
        )
    }

    @SubCommand(
        name = "clean-temporary-voice-channels",
        description = "Removes any left over temporary voice channel",
    )
    suspend fun cleanVcs(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext,
        @CommandOption(
            name = "hard-reset",
            description = "Whether Astro should wipe all the temporary vcs data",
            type = OptionType.BOOLEAN
        )
        hardReset: Boolean?
    ) {
        if (hardReset == true) {
            temporaryVCDao.deleteAll(ctx.guildId)
            ctx.replyHandler.replyEmbed(Embeds.default("Temporary voice channels have been reset"))
        } else {
            val vcs = temporaryVCDao.getAll(ctx.guildId).toMutableList()
            val leftOverVcIds = vcs.filter {
                ctx.guild.getVoiceChannelById(it.id)?.members?.isEmpty() == true
            }.map { it.id }

            if (leftOverVcIds.isNotEmpty()) {
                leftOverVcIds.forEach {
                    delay(1000)
                    ctx.guild.getVoiceChannelById(it)?.delete()?.queue()
                }

                vcs.removeAll { it.id in leftOverVcIds }
                temporaryVCDao.saveAll(ctx.guildId, vcs)

                ctx.replyHandler.replyEmbed(
                    Embeds.success(
                        "${leftOverVcIds.size} vcs have been removed from the server as Astro did not catch them while restarting or being offline."
                    )
                )
            } else {
                ctx.replyHandler.replyEmbed(
                    Embeds.success(
                        "This server doesn't have any left over voice channel :)"
                    )
                )
            }
        }
    }
}