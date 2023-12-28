package space.astro.shared.core.components.coroutine

import kotlinx.coroutines.CoroutineScope
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class CoroutineFactory(
    private val coroutineConfig: CoroutineConfig
) {
    @Bean
    fun applicationScope(): CoroutineScope = coroutineConfig.applicationCoroutineScope
}