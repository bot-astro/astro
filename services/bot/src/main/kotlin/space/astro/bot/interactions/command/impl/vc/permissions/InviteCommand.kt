package space.astro.bot.interactions.command.impl.vc.permissions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.interactions.command.*
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "invite",
    description = "Invite up to three users to join your VC by sending a DM with an invite link",
    category = CommandCategory.VC,
    premium = true
)
class InviteCommand: AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext,
        @CommandOption(
            name = "user",
            description = "The user to invite to join in your VC, he wi",
            type = OptionType.USER
        )
        member: Member,
        @CommandOption(
            name = "second-user",
            description = "Another user to invite to join in your VC, he wi",
            type = OptionType.USER,
        )
        member2: Member?,
        @CommandOption(
            name = "third-user",
            description = "Another user to invite to join in your VC, he wi",
            type = OptionType.USER
        )
        member3: Member?,
        @CommandOption(
            description = "A message that will be included in the DM",
            type = OptionType.STRING
        )
        message: String?
    ) {
        val members = listOfNotNull(member, member2, member3).filter { !it.user.isBot }

        if (members.isEmpty()) {
            event.replyEmbeds(Embeds.error(
                "None of the users you provided are in this server (you also can't invite bots)!"
            )).setEphemeral(true).queue()
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