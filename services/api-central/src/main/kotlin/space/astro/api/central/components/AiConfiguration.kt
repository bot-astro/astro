package space.astro.api.central.components

import ai.koog.http.client.HttpClientFactoryResolver
import ai.koog.prompt.executor.clients.modelsById
import ai.koog.prompt.executor.clients.openai.base.AbstractOpenAILLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterClientSettings
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.astro.api.central.properties.AiProperties
import space.astro.api.central.services.ai.AgentRunner
import space.astro.api.central.services.ai.AiSessionService
import space.astro.api.central.services.ai.AiTools
import space.astro.api.central.services.ai.KoogAgentRunner
import space.astro.api.central.services.ai.OpenRouterCostTrackingHttpClient

@Configuration
class AiConfiguration {
    @Bean
    fun agentRunner(
        aiProperties: AiProperties,
        aiSessionService: AiSessionService,
        aiTools: AiTools,
    ): AgentRunner =
        KoogAgentRunner(
            llmClient = openRouterClient(aiProperties),
            model = openRouterModel(aiProperties.model),
            maxSteps = aiProperties.maxSteps,
            sessionService = aiSessionService,
            toolExecutor = aiTools,
        )

    private fun openRouterClient(aiProperties: AiProperties): OpenRouterLLMClient {
        val settings = OpenRouterClientSettings(baseUrl = aiProperties.openrouterBaseUrl)
        return OpenRouterLLMClient(
            settings = settings,
            httpClient =
                OpenRouterCostTrackingHttpClient(
                    AbstractOpenAILLMClient.createConfiguredHttpClient(
                        apiKey = aiProperties.openrouterApiKey,
                        settings = settings,
                        httpClientFactory = HttpClientFactoryResolver.resolve(),
                        clientName = "OpenRouterLLMClient",
                    ),
                ),
        )
    }

    companion object {
        /**
         * Models unknown to Koog are allowed, OpenRouter accepts any id it serves
         */
        fun openRouterModel(id: String): LLModel =
            OpenRouterModels.modelsById()[id]
                ?: LLModel(
                    provider = LLMProvider.OpenRouter,
                    id = id,
                    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools, LLMCapability.Temperature),
                )
    }
}
