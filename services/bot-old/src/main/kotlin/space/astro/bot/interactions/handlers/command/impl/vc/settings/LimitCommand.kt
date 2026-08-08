package space.astro.bot.interactions.handlers.command.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "limit",
    description = "Set the user limit for your VC",
    category = CommandCategory.VC,
    action = InteractionAction.VC_LIMIT
)
class LimitCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext,
        @CommandOption(
            description = "The user limit for your VC (0 for no limit)",
            maxValue = 99,
            type = OptionType.INTEGER
        )
        limit: Int
    ) {
        val maxUserLimit = ctx.vcOperationCTX.generatorData.commandsSettings.maxUserLimit
        val minUserLimit = ctx.vcOperationCTX.generatorData.commandsSettings.minUserLimit

        if (limit < minUserLimit || limit > maxUserLimit) {
            ctx.replyHandler.replyEmbed(Embeds.error(
                "User limit must be between `$minUserLimit` and `$maxUserLimit`." +
                        "\n(*Those limits were set by the moderators of the server*)",
            ))
            return
        }

        ctx.replyHandler.deferReply()
        ctx.vcOperationCTX.temporaryVCManager.setUserLimit(limit).queue()

        ctx.replyHandler.replyEmbed(Embeds.default(
            "${Emojis.limit.formatted} User limit set to $limit users!"
        ))
    }
}