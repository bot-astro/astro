package space.astro.api.central.services.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import space.astro.shared.core.models.database.AiDiffEntry
import space.astro.shared.core.models.database.guildSettings.TemplateData
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

class AiProposalServiceTest {
    private val proposalService =
        AiProposalService(
            StringRedisTemplate(),
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build(),
        )

    private val template = TemplateData(id = "t1", name = "Gaming", vcLimit = 5)

    @Test
    fun `diff lists only the changed fields`() {
        val diff = proposalService.diff(template, template.copy(vcLimit = 10, vcName = "Game"))

        assertEquals(
            listOf(AiDiffEntry("vc_name", null, "Game"), AiDiffEntry("vc_limit", "5", "10")),
            diff.sortedBy { it.field }.reversed(),
        )
    }

    @Test
    fun `diff of a creation lists the set fields`() {
        val diff = proposalService.diff(null, template)

        assertEquals(setOf("id", "name", "vc_limit"), diff.map { it.field }.toSet())
        diff.forEach { assertNull(it.before) }
    }

    @Test
    fun `version changes when the target changes`() {
        assertEquals(proposalService.version(template), proposalService.version(template.copy()))
        assertNotEquals(proposalService.version(template), proposalService.version(template.copy(vcLimit = 6)))
        assertNull(proposalService.version(null))
    }
}
