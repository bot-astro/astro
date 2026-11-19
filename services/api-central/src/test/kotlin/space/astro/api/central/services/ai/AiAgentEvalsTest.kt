package space.astro.api.central.services.ai

import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import space.astro.api.central.components.AiConfiguration
import space.astro.shared.core.models.database.AiSurface

/**
 * "User says X, the model calls Y" cases run against the real model through OpenRouter, with canned tool results.
 * Run them after changing the prompt or the tools: `OPENROUTER_API_KEY=... ./gradlew :services:api-central:test -Pevals`
 */
@Tag("evals")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
class AiAgentEvalsTest {
    companion object {
        private val cannedResults =
            mapOf(
                "get_guild_overview" to
                    """{"locale":"en-US","premium":false,"generators":1,"interfaces":1,"templates":1,"voice_roles":1}""",
                "list_channels_and_categories" to
                    """
                    [{"id":"100","name":"Gaming","type":"category","category_id":null},
                     {"id":"101","name":"General","type":"voice","category_id":"100"},
                     {"id":"102","name":"lobby","type":"text","category_id":"100"},
                     {"id":"103","name":"Chill","type":"category","category_id":null}]
                    """.trimIndent(),
                "list_roles" to """[{"id":"200","name":"Member","assignable":true},{"id":"201","name":"VIP","assignable":true}]""",
                "list_generators" to
                    """
                    [{"generator_id":"300","channel_name":"➕ Join to create","default_name":"{nickname}'s VC",
                      "user_limit":null,"category_id":"100"}]
                    """.trimIndent(),
                "get_generator" to
                    """{"id":"300","default_name":"{nickname}'s VC","user_limit":null,"bitrate":null,"category":"100","auto_chat":false}""",
                "list_interfaces" to """[{"interface_id":"400","channel_id":"102","buttons":4}]""",
                "get_interface" to
                    """
                    {"message_id":"400","buttons":[
                      {"id":"lock","name":"Lock","button_disabled":false},{"id":"unlock","name":"Unlock","button_disabled":false},
                      {"id":"rename","name":"Rename","button_disabled":false},{"id":"limit","name":"Limit","button_disabled":false}]}
                    """.trimIndent(),
                "list_templates" to """[{"id":"t1","name":"Gaming","vc_name":"Gaming room","vc_limit":5}]""",
                "list_voice_roles" to """[{"id":"101","role_id":"201","action":"ASSIGN","permanent":false}]""",
                "get_premium_status" to
                    """{"premium":false,"free_max_generators":2,"free_max_interfaces":1,"free_max_templates":3}""",
            )

        @JvmStatic
        fun cases(): List<Arguments> =
            listOf(
                Arguments.of("Create a join to create voice channel called Squad in the Gaming category", "create_voice_generator"),
                Arguments.of("Add a generator named Duo with a user limit of 2", "create_voice_generator"),
                Arguments.of("Set the user limit of my generator to 5", "update_generator"),
                Arguments.of("Rename the temporary channels of my generator to {username}'s room", "update_generator"),
                Arguments.of("Make new temporary channels start locked", "update_generator"),
                Arguments.of("Delete my generator", "delete_generator"),
                Arguments.of("Send an interface in the lobby channel", "create_interface"),
                Arguments.of("Remove the rename button from my interface", "set_interface_buttons"),
                Arguments.of("Create a template called Study with a limit of 4 users", "create_template"),
                Arguments.of("Change the Gaming template limit to 8", "update_template"),
                Arguments.of("Give the Member role to people when they join the General voice channel", "create_voice_role"),
                Arguments.of("Delete the voice role of the VIP role in General", "delete_voice_role"),
                Arguments.of("Which generators do I have?", "list_generators"),
                Arguments.of("Does this server have premium?", "get_premium_status"),
                Arguments.of("What roles does the server have?", "list_roles"),
            )
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("cases")
    fun `the model calls the expected tool`(
        text: String,
        expectedTool: String,
    ) {
        val calledTools = mutableListOf<String>()
        val runner =
            KoogAgentRunner(
                llmClient = OpenRouterLLMClient(System.getenv("OPENROUTER_API_KEY")),
                model = AiConfiguration.openRouterModel(System.getenv("AI_MODEL") ?: "anthropic/claude-haiku-4.5"),
                maxSteps = 6,
                sessionService = null,
                toolExecutor = { _, name, _ ->
                    calledTools += name
                    AiToolOutput(
                        cannedResults[name]
                            ?: "Proposal p1 created. Nothing is saved yet, the user has to approve it with the button under your message.",
                    )
                },
            )

        val result = runner.runTurn(AiTurn(AiToolContext("1", "2", AiSurface.DASHBOARD), "", text)) { }

        assertTrue(expectedTool in calledTools, "expected $expectedTool, called $calledTools, answered: ${result.text}")
        assertTrue(result.inputTokens > 0, "the token usage is tracked")
    }
}
