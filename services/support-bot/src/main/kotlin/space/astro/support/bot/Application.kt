package space.astro.support.bot

import org.springframework.boot.actuate.autoconfigure.metrics.mongo.MongoMetricsAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "space.astro.support.bot",
        "space.astro.shared.core.configs",
        "space.astro.shared.core.components.coroutine",
        "space.astro.shared.core.components.io",
        "space.astro.shared.core.services.redis",
        "space.astro.shared.core.services.discord",
        "space.astro.shared.core.services.chargebee"
    ],
    exclude = [
        MongoAutoConfiguration::class,
        MongoMetricsAutoConfiguration::class
    ]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
