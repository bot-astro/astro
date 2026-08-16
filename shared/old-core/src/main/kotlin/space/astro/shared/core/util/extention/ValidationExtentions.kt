package space.astro.shared.core.util.extention

import space.astro.shared.core.util.validation.ValidationResult

fun Boolean.asValidationResult(message: String? = null) = if (this) ValidationResult.valid() else ValidationResult.invalid(message)

