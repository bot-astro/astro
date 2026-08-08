package space.astro.bot.interactions.handlers.button

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.InteractionContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

abstract class AbstractButton : space.astro.bot.interactions.handlers.button.IButton {

    final override var id: String
    final override var runnable: KFunction<*>?
    final override val action: InteractionAction

    init {
        val reflectedClass = this::class

        val buttonAnnotation = reflectedClass.findAnnotation<space.astro.bot.interactions.handlers.button.Button>()
            ?: throw UnsupportedOperationException("Missing button annotation on class extending AbstractButton!")

        id = buttonAnnotation.id
        action = buttonAnnotation.action

        val buttonRunnable = reflectedClass.memberFunctions.firstOrNull { it.hasAnnotation<space.astro.bot.interactions.handlers.button.ButtonRunnable>() }
            ?: throw UnsupportedOperationException("Missing MenuRunnable annotated function in class extending AbstractMenu")

        validateOptions(buttonRunnable)

        runnable = buttonRunnable
    }


    private fun validateOptions(
        function: KFunction<*>
    ) {
        if (function.parameters.size != 3) {
            throw UnsupportedOperationException("Function ${function.name} does not have two parameters!")
        }

        if (!(function.parameters[1].type.classifier as KClass<*>).isSubclassOf(ButtonInteractionEvent::class)) {
            throw UnsupportedOperationException("First parameter of ${function.name} must be a subclass of ButtonInteractionEvent!")
        }

        if (!function.parameters[2].type.isSubtypeOf(InteractionContext::class.createType())) {
            throw UnsupportedOperationException("Second parameter of ${function.name} must be a subtype of InteractionContext!")
        }
    }
}
