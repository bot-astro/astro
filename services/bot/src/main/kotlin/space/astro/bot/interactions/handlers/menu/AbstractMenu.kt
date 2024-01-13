package space.astro.bot.interactions.handlers.menu

import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.InteractionContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

abstract class AbstractMenu : IMenu {

    final override var id: String
    final override var runnable: KFunction<*>?
    final override val action: InteractionAction

    init {
        val reflectedClass = this::class

        val menuAnnotation = reflectedClass.findAnnotation<Menu>()
            ?: throw UnsupportedOperationException("Missing menu annotation on class extending AbstractMenu!")

        id = menuAnnotation.id
        action = menuAnnotation.action

        val menuRunnable = reflectedClass.memberFunctions.firstOrNull { it.hasAnnotation<MenuRunnable>() }
            ?: throw UnsupportedOperationException("Missing MenuRunnable annotated function in class extending AbstractMenu")

        validateOptions(menuRunnable)

        runnable = menuRunnable
    }


    private fun validateOptions(
        function: KFunction<*>
    ) {
        if (function.parameters.size != 3) {
            throw UnsupportedOperationException("Function ${function.name} does not have two parameters!")
        }

        if (!(function.parameters[1].type.classifier as KClass<*>).isSubclassOf(GenericSelectMenuInteractionEvent::class)) {
            throw UnsupportedOperationException("First parameter of ${function.name} must be a subclass of GenericSelectMenuInteractionEvent!")
        }

        if (!function.parameters[2].type.isSubtypeOf(InteractionContext::class.createType())) {
            throw UnsupportedOperationException("Second parameter of ${function.name} must be a subtype of InteractionContext!")
        }
    }
}