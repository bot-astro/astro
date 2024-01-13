package space.astro.bot.interactions.menu

import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionAction

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class Menu(
    val id: String,
    val action: InteractionAction = InteractionAction.GENERIC
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class MenuRunnable