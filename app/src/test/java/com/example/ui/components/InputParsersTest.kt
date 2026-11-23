package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class InputParsersTest {

    @Test
    fun `real dates parse`() {
        assertEquals("2026-12-05", parseIsoDateOrNull("2026-12-05"))
        assertEquals("2024-02-29", parseIsoDateOrNull(" 2024-02-29 "))
    }

    @Test
    fun `impossible dates are rejected`() {
        // A length check would accept all of these.
        assertNull(parseIsoDateOrNull("2024-02-31"))
        assertNull(parseIsoDateOrNull("2026-13-01"))
        assertNull(parseIsoDateOrNull("2026-00-10"))
        assertNull(parseIsoDateOrNull("2025-02-29"))
    }

    @Test
    fun `malformed input is rejected`() {
        assertNull(parseIsoDateOrNull(""))
        assertNull(parseIsoDateOrNull("next week"))
        assertNull(parseIsoDateOrNull("2026-1-5"))
        assertNull(parseIsoDateOrNull("05/12/2026"))
    }

    @Test
    fun `blank dates are allowed and produce no error`() {
        assertNull(isoDateError(""))
        assertNull(isoDateError("2026-12-05"))
        assertNotNull(isoDateError("2026-02-31"))
    }

    @Test
    fun `decimals accept a comma and reject nonsense`() {
        assertEquals(2.5, parseDecimalOrNull("2,5") { }!!, 0.0001)
        assertEquals(3.0, parseDecimalOrNull("3") { }!!, 0.0001)
        assertNull(parseDecimalOrNull("") { })
        var error: String? = null
        assertNull(parseDecimalOrNull("abc") { error = it })
        assertNotNull(error)
    }

    @Test
    fun `minutes reject fractions and negatives`() {
        assertEquals(30, parseMinutesOrNull("30") { })
        var error: String? = null
        assertNull(parseMinutesOrNull("1.5") { error = it })
        assertNotNull(error)
        assertNull(parseMinutesOrNull("-5") { })
        assertTrue(isValidMinutes("12"))
        assertFalse(isValidMinutes("1.5"))
        assertTrue(isValidMinutes(""))
    }
}
