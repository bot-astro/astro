package space.astro.bot.interactions.button

import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.springframework.stereotype.Component

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class Button(
    val id: String = "",
    val style: ButtonStyle = ButtonStyle.PRIMARY,
    val premium: Boolean = false
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ButtonRunnable