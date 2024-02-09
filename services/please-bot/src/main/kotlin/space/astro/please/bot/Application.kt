package space.astro.please.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "space.astro.please.bot",
        "space.astro.shared.core.components.redis",
    ]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
