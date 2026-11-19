package space.astro.api.central.services.ai

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import space.astro.api.central.models.ai.AiProposalDto
import tools.jackson.databind.json.JsonMapper
import java.io.OutputStream

/**
 * Writes a turn of the assistant in the AI SDK UI message stream protocol (v1), the format read by `useChat`
 *
 * @see <a href="https://ai-sdk.dev/docs/ai-sdk-ui/stream-protocol">UI message stream protocol</a>
 */
class AiUiMessageStreamWriter(
    private val outputStream: OutputStream,
    private val jsonMapper: JsonMapper,
) {
    companion object {
        const val HEADER_NAME = "x-vercel-ai-ui-message-stream"
        const val HEADER_VALUE = "v1"
    }

    private var openTextId: String? = null

    fun start() = write(mapOf("type" to "start", "messageId" to NanoIdUtils.randomNanoId()))

    /**
     * Forwards a turn event, consecutive text deltas are grouped in a single text part
     */
    fun event(event: AiTurnEvent) {
        when (event) {
            is AiTurnEvent.TextDelta -> {
                val textId =
                    openTextId ?: NanoIdUtils.randomNanoId().also {
                        openTextId = it
                        write(mapOf("type" to "text-start", "id" to it))
                    }
                write(mapOf("type" to "text-delta", "id" to textId, "delta" to event.text))
            }

            AiTurnEvent.StepStart -> write(mapOf("type" to "start-step"))

            AiTurnEvent.StepFinish -> {
                closeText()
                write(mapOf("type" to "finish-step"))
            }

            is AiTurnEvent.ToolCall -> {
                closeText()
                write(
                    mapOf(
                        "type" to "tool-input-available",
                        "toolCallId" to event.id,
                        "toolName" to event.name,
                        "input" to jsonMapper.readTree(event.input),
                    ),
                )
            }

            is AiTurnEvent.ToolResult ->
                write(mapOf("type" to "tool-output-available", "toolCallId" to event.id, "output" to event.output))

            is AiTurnEvent.Proposal ->
                write(mapOf("type" to "data-proposal", "id" to event.proposal.id, "data" to AiProposalDto.from(event.proposal)))
        }
    }

    fun text(text: String) {
        event(AiTurnEvent.TextDelta(text))
        closeText()
    }

    fun error(errorText: String) {
        closeText()
        write(mapOf("type" to "error", "errorText" to errorText))
    }

    fun finish() {
        closeText()
        write(mapOf("type" to "finish"))
        outputStream.write("data: [DONE]\n\n".toByteArray())
        outputStream.flush()
    }

    private fun closeText() {
        openTextId?.let { write(mapOf("type" to "text-end", "id" to it)) }
        openTextId = null
    }

    private fun write(chunk: Map<String, Any?>) {
        outputStream.write("data: ${jsonMapper.writeValueAsString(chunk)}\n\n".toByteArray())
        outputStream.flush()
    }
}
