package space.astro.shared.core.utils.validation

interface Validatable {
    fun validate(): ValidationResult
}