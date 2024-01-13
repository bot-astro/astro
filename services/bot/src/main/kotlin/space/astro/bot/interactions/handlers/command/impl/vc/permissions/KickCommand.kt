package space.astro.bot.interactions.handlers.command.impl.vc.permissions

/* Deprecated in favour of ban?
@Command(
    name = "ban",
    description = "Bans someone from joining your voice channel",
    category = CommandCategory.VC
)
class KickCommand : AbstractCommand() {
    @SubCommand(
        name = "user",
        description = "Kicks a user from your VC"
    )
    suspend fun user(
        event: SlashCommandInteractionEvent,
        @VcCommandContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcCommandContext,
        @CommandOption(
            type = OptionType.USER,
            name = "user",
            description = "The user to ban from your channel"
        ) member: Member?,
    ) {
        if (member == null) {
            event.replyEmbeds(Embeds.error("The user you provided is not in this server!"))
                .setEphemeral(true).queue()
            return
        }

        val immuneRoleId = ctx.vcOperationCTX.generatorData.permissionsImmuneRole

        if (immuneRoleId != null && member.roles.any { it.id == immuneRoleId }) {
            event.replyEmbeds(Embeds.error("${member.asMention} has the ${immuneRoleId.asRoleMention()} role which is immune to this command!"))
                .setEphemeral(true).queue()
            return
        }

        if (member.voiceState!!.channel?.id == ctx.vcOperationCTX.temporaryVC.id) {
            ctx.guild.kickVoiceMember(member).queue()
        }

        ctx.vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
            member,
            0,
            Permission.VOICE_CONNECT.rawValue
        ).queue()

        event.replyEmbeds(Embeds.default("${member.asMention} has been banned from your temporary VC!"))
            .setEphemeral(true).queue()
    }

    @SubCommand(
        name = "role",
        description = "Bans a role from your VC"
    )
    suspend fun role(
        event: SlashCommandInteractionEvent,
        @VcCommandContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcCommandContext,
        @CommandOption(
            type = OptionType.USER,
            name = "role",
            description = "The role to ban from your channel"
        ) role: Role,
    ) {
        val immuneRoleId = ctx.vcOperationCTX.generatorData.permissionsImmuneRole

        if (immuneRoleId == role.id) {
            event.replyEmbeds(Embeds.error("The ${immuneRoleId.asRoleMention()} role is immune to this command!"))
                .setEphemeral(true).queue()
            return
        }

        ctx.vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
            role,
            0,
            Permission.VOICE_CONNECT.rawValue
        ).queue()

        event.replyEmbeds(Embeds.default("${role.asMention} role has been ${Emojis.ban.formatted} banned from your temporary VC!"))
            .setEphemeral(true).queue()
    }
}
 */