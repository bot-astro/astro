package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import net.dv8tion.jda.api.Permission
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.models.ai.AiInternalProposalBody
import space.astro.api.central.models.ai.AiInternalTurnBody
import space.astro.api.central.models.ai.AiInternalTurnResponse
import space.astro.api.central.models.ai.AiProposalApplyResponse
import space.astro.api.central.models.ai.AiProposalDto
import space.astro.api.central.properties.DiscordApplicationProperties
import space.astro.api.central.services.ai.AiChatService
import space.astro.api.central.services.ai.AiProposalApplyService
import space.astro.api.central.services.ai.AiToolContext
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.models.database.AiSurface
import space.astro.shared.core.utils.api.CentralApiEndpoint
import space.astro.shared.core.utils.redis.RedisKey

/**
 * Assistant endpoints called by the bot for the Discord surface, authenticated with the internal token.
 * The bot is trusted for the user id, the permissions of the user are still checked on Discord.
 */
@RestController
@Tag(name = "AI assistant (internal)")
class InternalAiController(
    private val aiChatService: AiChatService,
    private val aiProposalApplyService: AiProposalApplyService,
    private val discordApiClient: DiscordApiClient,
    private val discordApplicationProperties: DiscordApplicationProperties,
) {
    @Operation(summary = "Run a turn of the assistant for a Discord thread")
    @PostMapping(CentralApiEndpoint.INTERNAL_AI_TURN)
    fun turn(
        @PathVariable guildId: String,
        @RequestBody body: AiInternalTurnBody,
    ): ResponseEntity<AiInternalTurnResponse> {
        requireManageServer(guildId, body.userId)

        val result =
            aiChatService.runTurn(
                AiToolContext(guildId, body.userId, AiSurface.DISCORD),
                // one history per thread, a new /configure thread starts a new conversation
                RedisKey.AI_SESSION(guildId, body.userId, "discord:${body.threadId}"),
                body.text,
            ) { }

        return ResponseEntity.ok(
            AiInternalTurnResponse(
                text = result.text,
                proposals = result.proposals.map { AiProposalDto.from(it) },
                limitReached = result.limitReached,
            ),
        )
    }

    @Operation(summary = "Apply a proposal of the assistant for a Discord user")
    @PostMapping(CentralApiEndpoint.INTERNAL_AI_PROPOSAL_APPLY)
    fun applyProposal(
        @PathVariable guildId: String,
        @PathVariable proposalId: String,
        @RequestBody body: AiInternalProposalBody,
    ): ResponseEntity<AiProposalApplyResponse> {
        requireManageServer(guildId, body.userId)
        val guildSettings = aiProposalApplyService.apply(guildId, body.userId, proposalId)
        return ResponseEntity.ok(AiProposalApplyResponse(status = "applied", result = guildSettings))
    }

    @Operation(summary = "Cancel a proposal of the assistant for a Discord user")
    @PostMapping(CentralApiEndpoint.INTERNAL_AI_PROPOSAL_CANCEL)
    fun cancelProposal(
        @PathVariable guildId: String,
        @PathVariable proposalId: String,
        @RequestBody body: AiInternalProposalBody,
    ): ResponseEntity<Void> {
        aiProposalApplyService.cancel(guildId, body.userId, proposalId)
        return ResponseEntity.noContent().build()
    }

    /**
     * Computes the guild-level permissions of the member from its roles, the owner can always manage the server
     *
     * @throws AUnauthorizedException
     */
    private fun requireManageServer(
        guildId: String,
        userId: String,
    ) {
        val guild = discordApiClient.getGuild(discordApplicationProperties.token, guildId)
        if (guild.ownerId == userId) {
            return
        }

        val member = discordApiClient.getGuildMember(discordApplicationProperties.token, guildId, userId)
        // the @everyone role has the id of the guild
        val permissions =
            guild.roles
                .filter { it.id == guildId || it.id in member.roles }
                .fold(0L) { acc, role -> acc or role.permissions.toLong() }

        if (permissions and (Permission.ADMINISTRATOR.rawValue or Permission.MANAGE_SERVER.rawValue) == 0L) {
            throw AUnauthorizedException("Managing Astro with the assistant requires the Manage Server permission")
        }
    }
}
