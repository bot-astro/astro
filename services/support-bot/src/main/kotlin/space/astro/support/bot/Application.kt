package space.astro.support.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "space.astro.support.bot",
        "space.astro.shared.core.configs",
        "space.astro.shared.core.services.redis"
    ]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
