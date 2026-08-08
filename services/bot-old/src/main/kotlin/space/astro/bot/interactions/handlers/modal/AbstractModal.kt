package space.astro.bot.interactions.handlers.modal

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.InteractionContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

abstract class AbstractModal : IModal {

    final override var id: String
    final override var runnable: KFunction<*>?
    final override var action: InteractionAction

    init {
        val reflectedClass = this::class

        val modalAnnotation = reflectedClass.findAnnotation<Modal>()
            ?: throw UnsupportedOperationException("Missing Modal annotation on class extending AbstractModal!")

        id = modalAnnotation.id
        action = modalAnnotation.action

        val modalRunnable = reflectedClass.memberFunctions.firstOrNull { it.hasAnnotation<ModalRunnable>() }
            ?: throw UnsupportedOperationException("Missing MenuRunnable annotated function in class extending AbstractMenu")

        validateOptions(modalRunnable)

        runnable = modalRunnable
    }


    private fun validateOptions(
        function: KFunction<*>
    ) {
        if (function.parameters.size != 3) {
            throw UnsupportedOperationException("Function ${function.name} does not have two parameters!")
        }

        if (!(function.parameters[1].type.classifier as KClass<*>).isSubclassOf(ModalInteractionEvent::class)) {
            throw UnsupportedOperationException("First parameter of ${function.name} must be a subclass of ModalInteractionEvent!")
        }

        if (!function.parameters[2].type.isSubtypeOf(InteractionContext::class.createType())) {
            throw UnsupportedOperationException("Second parameter of ${function.name} must be a subtype of InteractionContext!")
        }
    }
}
