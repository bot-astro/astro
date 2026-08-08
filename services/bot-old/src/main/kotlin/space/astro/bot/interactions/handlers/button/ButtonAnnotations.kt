package space.astro.bot.interactions.handlers.button

import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionAction

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class Button(
    val id: String = "",
    val action: InteractionAction = InteractionAction.GENERIC
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ButtonRunnable