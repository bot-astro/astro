package space.astro.shared.core.components.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig {
    val applicationCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}