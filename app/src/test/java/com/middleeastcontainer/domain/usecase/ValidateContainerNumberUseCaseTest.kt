package com.middleeastcontainer.domain.usecase

import com.middleeastcontainer.domain.usecase.ValidateContainerNumberUseCase.Reason
import com.middleeastcontainer.domain.usecase.ValidateContainerNumberUseCase.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ISO 6346 oracle. Vectors were verified offline against the legacy algorithm and
 * against the textbook example CSQU3054383. These lock the accept/reject boundary
 * so the new client matches the legacy app character-for-character.
 */
class ValidateContainerNumberUseCaseTest {

    private val validate = ValidateContainerNumberUseCase()

    private val validNumbers = listOf(
        "CSQU3054383", // classic ISO 6346 example, check digit 3
        "MSKU0000006",
        "HLXU1234561",
        "TCLU1234568",
        "APLU4567893",
        "MAEU2000007",
        "OOLU8765436",
        "TGHU1111114",
        "GATU0000000",
    )

    @Test
    fun `accepts known-valid container numbers`() {
        validNumbers.forEach { n ->
            assertEquals("expected $n to be valid", Result.Valid, validate(n))
        }
    }

    @Test
    fun `rejects a wrong check digit`() {
        assertEquals(Result.Invalid(Reason.BAD_CHECK_DIGIT), validate("CSQU3054384"))
    }

    @Test
    fun `rejects wrong length`() {
        assertEquals(Result.Invalid(Reason.WRONG_LENGTH), validate("CSQU305438"))    // 10
        assertEquals(Result.Invalid(Reason.WRONG_LENGTH), validate("CSQU30543833"))  // 12
    }

    @Test
    fun `rejects non-letter in owner code`() {
        assertEquals(Result.Invalid(Reason.BAD_OWNER_LETTERS), validate("1SQU3054383"))
    }

    @Test
    fun `rejects illegal character`() {
        assertEquals(Result.Invalid(Reason.BAD_OWNER_LETTERS), validate("CS#U3054383"))
    }

    @Test
    fun `rejects letter where a serial digit is required`() {
        assertEquals(Result.Invalid(Reason.BAD_SERIAL_DIGITS), validate("CSQUA054383"))
    }

    @Test
    fun `rejects lowercase owner letters (legacy required uppercase)`() {
        assertTrue(validate("csqu3054383") is Result.Invalid)
    }

    @Test
    fun `check digit helper computes the textbook example`() {
        assertEquals(3, validate.checkDigitFor("CSQU305438"))
    }
}
