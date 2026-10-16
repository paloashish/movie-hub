package com.moviehub.movie.api.graphql

import io.mockk.mockk
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Year

class MaxYearsAheadValidatorTest {

    private val thisYear = Year.now().value
    private val validator = MaxYearsAheadValidator().apply { initialize(MaxYearsAhead(5)) }
    private val context = mockk<ConstraintValidatorContext>()

    @Test
    fun `should accept this year`() {
        assertTrue(validator.isValid(thisYear, context))
    }

    @Test
    fun `should accept exactly five years ahead`() {
        assertTrue(validator.isValid(thisYear + 5, context))
    }

    @Test
    fun `should reject six years ahead`() {
        assertFalse(validator.isValid(thisYear + 6, context))
    }

    @Test
    fun `should not reject a missing value, as Bean Validation convention requires`() {
        assertTrue(validator.isValid(null, context))
    }
}
