package space.astro.bot.interactions.handlers.menu.impl.vc.permissions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.menu.AbstractMenu
import space.astro.bot.interactions.handlers.menu.Menu
import space.astro.bot.interactions.handlers.menu.MenuRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Menu(
    id = InteractionIds.Menu.VC_INVITE,
    action = InteractionAction.VC_INVITE
)
class InviteMenu: AbstractMenu() {
    @MenuRunnable
    suspend fun run(
        event: EntitySelectInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        val members = event.values.mapNotNull {
            selectValue -> ctx.guild.getMemberById(selectValue.id)
                ?.takeIf { !it.user.isBot }
        }

        if (members.isEmpty()) {
            event.hook.editOriginalEmbeds(Embeds.error(
                "None of the users you provided are in this server (you also can't invite bots)!"
            )).queue()

            return
        }

        event.deferReply(true).await()

        val vcInvite = ctx.vcOperationCTX.temporaryVC.retrieveInvites().await().firstOrNull()
            ?: ctx.vcOperationCTX.temporaryVC.createInvite().await()

        members.forEach {
            ctx.vcOperationCTX.temporaryVCManager.modifyPermissionOverride(
                it,
                Permission.getRaw(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
                0
            )

            it.user.openPrivateChannel()
                .queue {privateChannel ->
                    privateChannel.sendMessageEmbeds(Embeds.default("${ctx.member.asMention} has invited you to join his VC in ${ctx.guild.name}!" +
                            "\n[Accept invitation](${vcInvite.url})"))
                        .queue()
                }
        }

        ctx.vcOperationCTX.temporaryVCManager.queue()

        event.hook.editOriginalEmbeds(Embeds.default(
            "${members.joinToString(", ") { it.asMention }} has been invited to join your VC!"
        )).queue()
    }
}