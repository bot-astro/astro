package space.astro.bot.interactions.handlers.command.impl.predashboard

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
    action = InteractionAction.HIGH_COOLDOWN_NO_ADMIN
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
                "\n\n" +
                errors.joinToString("\n\n") {
                    it.description + if (it.instant != null) "\n> ${simpleDateFormat.format(Date.from(it.instant!!))}" else ""
                }

        ctx.replyHandler.replyEmbed(
            Embeds.default(description.take(MessageEmbed.DESCRIPTION_MAX_LENGTH))
        )
    }

    @SubCommand(
        name = "reset-temporary-voice-channels",
        description = "Resets Astro temporary voice channels data",
    )
    suspend fun resetVcs(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext,
    ) {
        temporaryVCDao.deleteAll(ctx.guildId)
        ctx.replyHandler.replyEmbed(Embeds.default("Temporary voice channels have been reset"))
    }
}