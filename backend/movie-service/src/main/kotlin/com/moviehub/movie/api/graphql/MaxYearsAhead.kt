package com.moviehub.movie.api.graphql

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.time.Year
import kotlin.reflect.KClass

/**
 * A release year may be in the near future (announced titles) but not absurdly so (application-architecture 4.2).
 * Bean Validation has @Min for the lower bound; the upper bound moves with the calendar, so it needs this.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MaxYearsAheadValidator::class])
annotation class MaxYearsAhead(
    val value: Int,
    val message: String = "must not be more than {value} years in the future",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class MaxYearsAheadValidator : ConstraintValidator<MaxYearsAhead, Int> {
    private var yearsAhead = 0

    override fun initialize(annotation: MaxYearsAhead) {
        yearsAhead = annotation.value
    }

    // Bean Validation convention: a validator never rejects null, a @NotNull would if the field allowed it
    override fun isValid(year: Int?, context: ConstraintValidatorContext): Boolean =
        year == null || year <= Year.now().value + yearsAhead
}
