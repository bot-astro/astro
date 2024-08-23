package space.astro.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "space.astro.bot",
        "space.astro.shared.core.configs",
        "space.astro.shared.core.components.io",
        "space.astro.shared.core.components.managers",
        "space.astro.shared.core.components.bigquery",
        "space.astro.shared.core.services.redis",
        "space.astro.shared.core.components.kmongo",
        "space.astro.shared.core.components.influx",
        "space.astro.shared.core.daos",
        "space.astro.shared.core.services",
        "space.astro.shared.core.services.chargebee",
        "space.astro.shared.core.services.support"
    ]
)
class Application

// I need this built
fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
