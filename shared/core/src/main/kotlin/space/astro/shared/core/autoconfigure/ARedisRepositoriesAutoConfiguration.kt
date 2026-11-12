package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.redis.core.StringRedisTemplate

@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate::class)
@AutoConfigureAfter(ARedisAutoConfiguration::class)
@ComponentScan(
    basePackages = ["space.astro.shared.core.repositories.redis"],
)
class ARedisRepositoriesAutoConfiguration
