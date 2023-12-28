package space.astro.api.central

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "space.astro.api.central",
        "space.astro.shared.core.configs",
        "space.astro.shared.core.components.io",
        "space.astro.shared.core.components.redis",
        "space.astro.shared.core.components.mongo"
    ]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
