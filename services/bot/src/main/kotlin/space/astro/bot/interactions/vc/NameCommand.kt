package space.astro.bot.interactions.vc

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCNameManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Embeds
import space.astro.bot.ui.Emojis

@Component
class NameCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val vcNameManager: VCNameManager,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val NAME_OPTION = "name"
    }

    override val data =
        Commands
            .slash("name", "Set the name for your VC")
            .setContexts(InteractionContextType.GUILD)
            .addOption(OptionType.STRING, NAME_OPTION, "The name for your VC", true, false)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.USER_RENAME,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val name = event.getOption(NAME_OPTION)?.asString

        if (name.isNullOrBlank()) {
            ctx.responder.error("Please provide a valid name")
            return
        }

        vcNameManager.performVCRename(ctx.vcOperationCTX, name)
        ctx.vcOperationCTX.queueUpdatedManagers()

        ctx.responder.reply(Embeds.default("${Emojis.name.formatted} Name of your VC set to `$name`!"))
    }
}
