package space.astro.api.central.services.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import space.astro.api.central.models.ai.AiProposal
import space.astro.shared.core.models.database.AiDiffEntry
import space.astro.shared.core.models.database.AiSurface
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.ByteArrayOutputStream

class AiUiMessageStreamWriterTest {
    private val jsonMapper =
        JsonMapper
            .builder()
            .addModule(kotlinModule())
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()

    @Test
    fun `writes a turn in the UI message stream protocol`() {
        val output = ByteArrayOutputStream()
        val writer = AiUiMessageStreamWriter(output, jsonMapper)
        val proposal =
            AiProposal(
                id = "p1",
                guildId = "1",
                userId = "2",
                surface = AiSurface.DASHBOARD,
                action = "update_generator",
                summary = "Update the generator",
                targetId = "3",
                payload = null,
                diff = listOf(AiDiffEntry("user_limit", "2", "5")),
                baseVersion = null,
                expiresAt = 0,
            )

        writer.start()
        writer.event(AiTurnEvent.StepStart)
        writer.event(AiTurnEvent.TextDelta("Hel"))
        writer.event(AiTurnEvent.TextDelta("lo"))
        writer.event(AiTurnEvent.ToolCall("c1", "update_generator", """{"generator_id":"3"}"""))
        writer.event(AiTurnEvent.Proposal(proposal))
        writer.event(AiTurnEvent.ToolResult("c1", "done"))
        writer.event(AiTurnEvent.StepFinish)
        writer.error("Limit reached")
        writer.finish()

        val chunks = output.toString().split("\n\n").filter { it.isNotEmpty() }
        chunks.forEach { assertTrue(it.startsWith("data: "), it) }
        val payloads = chunks.map { it.removePrefix("data: ") }
        assertEquals("[DONE]", payloads.last())

        val parsed = payloads.dropLast(1).map { jsonMapper.readTree(it) }
        assertEquals(
            listOf(
                "start",
                "start-step",
                "text-start",
                "text-delta",
                "text-delta",
                "text-end",
                "tool-input-available",
                "data-proposal",
                "tool-output-available",
                "finish-step",
                "error",
                "finish",
            ),
            parsed.map { it.get("type").asString() },
        )
        assertEquals(parsed[2].get("id"), parsed[3].get("id"))
        assertEquals("lo", parsed[4].get("delta").asString())
        assertEquals("3", parsed[6].get("input").get("generator_id").asString())
        assertEquals("c1", parsed[6].get("toolCallId").asString())
        val proposalData = parsed[7].get("data")
        assertEquals("p1", proposalData.get("id").asString())
        assertEquals("1970-01-01T00:00:00Z", proposalData.get("expiresAt").asString())
        assertEquals(
            "user_limit",
            proposalData
                .get("diff")
                .get(0)
                .get("field")
                .asString(),
        )
        assertEquals("Limit reached", parsed[10].get("errorText").asString())
    }
}
