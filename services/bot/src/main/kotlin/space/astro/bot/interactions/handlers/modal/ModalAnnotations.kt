package space.astro.bot.interactions.handlers.modal

import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionAction

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class Modal(
    val id: String = "",
    val action: InteractionAction = InteractionAction.GENERIC
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ModalRunnable