package space.astro.api.central.controllers.guild_settings

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.ai.AiChatBody
import space.astro.api.central.models.ai.AiFormFillBody
import space.astro.api.central.models.ai.AiFormFillResponse
import space.astro.api.central.models.ai.AiProposalApplyResponse
import space.astro.api.central.models.ai.AiUsageResponse
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.services.ai.AiChatService
import space.astro.api.central.services.ai.AiProposalApplyService
import space.astro.api.central.services.ai.AiToolContext
import space.astro.api.central.services.ai.AiUiMessageStreamWriter
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.models.database.AiSurface
import space.astro.shared.core.utils.api.CentralApiEndpoint
import space.astro.shared.core.utils.redis.RedisKey
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger { }

@RestController
@Tag(name = "AI assistant")
class GuildAiController(
    private val aiChatService: AiChatService,
    private val aiProposalApplyService: AiProposalApplyService,
    private val jsonMapper: JsonMapper,
) {
    @Operation(
        summary = "Chat with the assistant",
        description =
            "Takes the request of the AI SDK useChat, only the last user message is used as the history is kept on the server. " +
                "Answers with a UI message stream (SSE), proposals are sent as `data-proposal` parts",
    )
    @PostMapping(CentralApiEndpoint.GUILD_AI_CHAT, produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @CanManageGuildMiddleware
    fun chat(
        @PathVariable guildId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
        @RequestBody body: AiChatBody,
    ): ResponseEntity<StreamingResponseBody> {
        val text = body.lastUserText() ?: throw ABadRequestException("The request has no user message")
        val context = AiToolContext(guildId, authPrincipal.userId, AiSurface.DASHBOARD)

        val stream =
            StreamingResponseBody { outputStream ->
                val writer = AiUiMessageStreamWriter(outputStream, jsonMapper)
                writer.start()
                try {
                    val result =
                        aiChatService.runTurn(
                            context,
                            RedisKey.AI_SESSION(guildId, authPrincipal.userId, "dashboard"),
                            text,
                            writer::event,
                        )
                    if (result.limitReached) {
                        writer.error(result.text)
                    }
                } catch (e: AException) {
                    writer.error(e.message ?: "The assistant failed to answer, try again later")
                } catch (e: Exception) {
                    log.error(e) { "AI chat turn failed for guild $guildId" }
                    writer.error("The assistant failed to answer, try again later")
                }
                writer.finish()
            }

        return ResponseEntity
            .ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .header(AiUiMessageStreamWriter.HEADER_NAME, AiUiMessageStreamWriter.HEADER_VALUE)
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            // keeps proxies like nginx from buffering the stream
            .header("X-Accel-Buffering", "no")
            .body(stream)
    }

    @Operation(
        summary = "Apply a proposal of the assistant",
        description = "Writes the exact change of the proposal, 409 if it expired or the settings changed since it was made",
    )
    @PostMapping(CentralApiEndpoint.GUILD_AI_PROPOSAL_APPLY)
    @CanManageGuildMiddleware
    fun applyProposal(
        @PathVariable guildId: String,
        @PathVariable proposalId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<AiProposalApplyResponse> {
        val guildSettings = aiProposalApplyService.apply(guildId, authPrincipal.userId, proposalId)
        return ResponseEntity.ok(AiProposalApplyResponse(status = "applied", result = guildSettings))
    }

    @Operation(summary = "Cancel a proposal of the assistant")
    @PostMapping(CentralApiEndpoint.GUILD_AI_PROPOSAL_CANCEL)
    @CanManageGuildMiddleware
    fun cancelProposal(
        @PathVariable guildId: String,
        @PathVariable proposalId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<Void> {
        aiProposalApplyService.cancel(guildId, authPrincipal.userId, proposalId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Fill a settings form with the assistant",
        description = "Returns the new form values for the request, nothing is saved",
    )
    @PostMapping(CentralApiEndpoint.GUILD_AI_FORM_FILL)
    @CanManageGuildMiddleware
    fun fillForm(
        @PathVariable guildId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
        @RequestBody body: AiFormFillBody,
    ): ResponseEntity<AiFormFillResponse> =
        ResponseEntity.ok(
            aiChatService.fillForm(AiToolContext(guildId, authPrincipal.userId, AiSurface.DASHBOARD), body),
        )

    @Operation(summary = "Monthly usage of the assistant")
    @GetMapping(CentralApiEndpoint.GUILD_AI_USAGE)
    @CanManageGuildMiddleware
    fun getUsage(
        @PathVariable guildId: String,
    ): ResponseEntity<AiUsageResponse> = ResponseEntity.ok(aiChatService.getUsage(guildId))
}
