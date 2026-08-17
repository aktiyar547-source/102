package com.middleeastcontainer.domain.usecase

import javax.inject.Inject

/**
 * Validates a shipping-container number against ISO 6346, reproducing the legacy
 * app's acceptance rule character-for-character so the new client accepts/rejects
 * exactly the same inputs as the app it replaces.
 *
 * Legacy rule (from OcrActivity: outer field checks + verifynumber):
 *   - length must be exactly 11
 *   - positions 0..3 : uppercase letters A-Z
 *   - positions 4..10: digits 0-9
 *   - ISO 6346 check digit: check == (sum % 11) % 10, where
 *       sum = Σ value(char_i) * 2^i   for i in 0..9
 *     and value() indexes the ISO 6346 table below (letters skip 11/22/33).
 *
 * Verified offline against known-valid numbers (e.g. CSQU3054383 → CD 3).
 */
class ValidateContainerNumberUseCase @Inject constructor() {

    sealed interface Result {
        data object Valid : Result
        data class Invalid(val reason: Reason) : Result
    }

    enum class Reason { WRONG_LENGTH, BAD_OWNER_LETTERS, BAD_SERIAL_DIGITS, BAD_CHECK_DIGIT, ILLEGAL_CHARACTER }

    operator fun invoke(raw: String): Result {
        val value = raw.trim()
        if (value.length != LENGTH) return Result.Invalid(Reason.WRONG_LENGTH)

        // Positions 0..3 must be uppercase A-Z (legacy used Character.isUpperCase).
        for (i in 0 until OWNER_LEN) {
            val ch = value[i]
            if (ch !in 'A'..'Z') return Result.Invalid(Reason.BAD_OWNER_LETTERS)
        }
        // Positions 4..10 must be digits (legacy used Character.isDigit on 4..10).
        for (i in OWNER_LEN until LENGTH) {
            if (!value[i].isDigit()) return Result.Invalid(Reason.BAD_SERIAL_DIGITS)
        }

        return when (isValidCheckDigit(value)) {
            true -> Result.Valid
            false -> Result.Invalid(Reason.BAD_CHECK_DIGIT)
            null -> Result.Invalid(Reason.ILLEGAL_CHARACTER)
        }
    }

    /** @return true/false for check-digit result, or null if a character is off-table. */
    private fun isValidCheckDigit(value: String): Boolean? {
        val upper = value.uppercase()
        var sum = 0L
        for (i in 0 until CHECK_INDEX) {
            val idx = TABLE.indexOf(upper[i])
            if (idx < 0) return null
            sum += idx.toLong() * POW2[i]
        }
        val expected = ((sum % 11) % 10).toInt()
        val actual = upper[CHECK_INDEX].digitToIntOrNull() ?: return null
        return expected == actual
    }

    /** Computes the correct check digit for a 10-char prefix (owner+serial); null if illegal. */
    fun checkDigitFor(prefix10: String): Int? {
        if (prefix10.length != CHECK_INDEX) return null
        val upper = prefix10.uppercase()
        var sum = 0L
        for (i in 0 until CHECK_INDEX) {
            val idx = TABLE.indexOf(upper[i])
            if (idx < 0) return null
            sum += idx.toLong() * POW2[i]
        }
        return ((sum % 11) % 10).toInt()
    }

    companion object {
        private const val LENGTH = 11
        private const val OWNER_LEN = 4
        private const val CHECK_INDEX = 10

        /** ISO 6346 value table; '?' marks the skipped values at 11, 22, 33. */
        private const val TABLE = "0123456789A?BCDEFGHIJK?LMNOPQRSTU?VWXYZ"

        private val POW2 = LongArray(10) { 1L shl it } // 2^0 .. 2^9
    }
}
