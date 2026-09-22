package space.astro.shared.core.utils.validation

import space.astro.shared.core.exceptions.ABadRequestException

data class ValidationResult(
    val isValid: Boolean,
    val invalidMessage: String? = null
) {
    companion object {
        fun valid(): ValidationResult {
            return ValidationResult(true)
        }

        fun invalid(message: String?): ValidationResult {
            return ValidationResult(false, message)
        }

        fun combine(vararg results: ValidationResult): ValidationResult {
            return results.firstOrNull { !it.isValid } ?: valid()
        }
    }

    /**
     * Throws [ABadRequestException] if not valid
     */
    fun throwIfInvalid() {
        if (!isValid)
            throw ABadRequestException(message = invalidMessage ?: "Invalid values")
    }
}
