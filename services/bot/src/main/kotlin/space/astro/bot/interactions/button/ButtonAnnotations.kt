package space.astro.bot.interactions.button

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.springframework.stereotype.Component
import space.astro.bot.interactions.command.CommandCategory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class Button(
    val name: String = "",
    val style: ButtonStyle = ButtonStyle.PRIMARY,
    val requiredPermissions: Array<Permission> = [],
    val category: CommandCategory = CommandCategory.ALL,
    val premium: Boolean = false
)