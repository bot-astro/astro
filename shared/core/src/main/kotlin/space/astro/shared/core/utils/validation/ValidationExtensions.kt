package space.astro.shared.core.utils.validation

fun Boolean.asValidationResult(message: String? = null) = if (this) ValidationResult.valid() else ValidationResult.invalid(message)