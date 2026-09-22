package space.astro.api.central.components

import org.springframework.core.MethodParameter
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter
import space.astro.shared.core.utils.validation.Validatable
import java.lang.reflect.Type

@ControllerAdvice
class ValidatableControllerAdvice : RequestBodyAdviceAdapter() {

    override fun supports(
        methodParameter: MethodParameter,
        targetType: Type,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = Validatable::class.java.isAssignableFrom(methodParameter.parameterType)

    override fun afterBodyRead(
        body: Any,
        inputMessage: HttpInputMessage,
        parameter: MethodParameter,
        targetType: Type,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Any {
        val result = (body as Validatable).validate()
        result.throwIfInvalid()
        return body
    }
}