package space.astro.bot.commands.vc.permissions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Embeds

@Component
class InviteCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private val USER_OPTIONS = listOf("user", "second-user", "third-user")
    }

    override val data =
        Commands
            .slash("invite", "Invite up to three users to join your VC by sending a DM with an invite link")
            .setContexts(InteractionContextType.GUILD)
            .addOption(OptionType.USER, USER_OPTIONS[0], "The user to invite to join your VC", true)
            .addOption(OptionType.USER, USER_OPTIONS[1], "Another user to invite to join your VC", false)
            .addOption(OptionType.USER, USER_OPTIONS[2], "Another user to invite to join your VC", false)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.UNKNOWN,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val members =
            USER_OPTIONS
                .mapNotNull { event.getOption(it)?.asMember }
                .filter { !it.user.isBot }
                .distinct()

        if (members.isEmpty()) {
            ctx.responder.error("None of the users you provided are in this server (you also can't invite bots)!")
            return
        }

        val vcCtx = ctx.vcOperationCTX

        val vcInvite =
            vcCtx.temporaryVC
                .retrieveInvites()
                .await()
                .firstOrNull()
                ?: vcCtx.temporaryVC.createInvite().await()

        vcPermissionManager.permitMembers(vcCtx, members)
        vcCtx.queueUpdatedManagers()

        members.forEach { member ->
            member.user.openPrivateChannel().queue({ privateChannel ->
                privateChannel
                    .sendMessageEmbeds(
                        Embeds.default(
                            "${ctx.member.asMention} has invited you to join their VC in ${ctx.guild.name}!" +
                                "\n[Accept invitation](${vcInvite.url})",
                        ),
                    ).queue(null) { }
            }) { }
        }

        ctx.responder.reply(Embeds.default("${members.joinToString(", ") { it.asMention }} has been invited to join your VC!"))
    }
}
