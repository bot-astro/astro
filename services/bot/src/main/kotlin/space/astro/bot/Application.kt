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
        "space.astro.shared.core.components.bigquery",
        "space.astro.shared.core.components.redis",
        "space.astro.shared.core.components.mongo",
        "space.astro.shared.core.daos",
        "space.astro.shared.core.services.redis",
    ]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}