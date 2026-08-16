package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.ui.screens.currencyOptions
import com.jimz011apps.hki7.ui.screens.currencySymbolFor
import com.jimz011apps.hki7.ui.screens.formatEnergyCost
import com.jimz011apps.hki7.ui.screens.isoCurrencyOrNull
import com.jimz011apps.hki7.ui.screens.resolveCurrencyToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class EnergyCurrencyTest {
    private val dutch = Locale.forLanguageTag("nl-NL")
    private val american = Locale.forLanguageTag("en-US")

    /** Digits only; the grouping/decimal marks differ per locale and are asserted separately. */
    private fun digits(text: String) = text.filter { it.isDigit() }

    /** Locale patterns separate the symbol from the amount with a non-breaking space. */
    private fun plainSpaces(text: String) = text.map { if (it.isWhitespace()) ' ' else it }.joinToString("")

    @Test
    fun `manual choice wins over the sensor unit, which wins over the locale`() {
        assertEquals("SEK", resolveCurrencyToken("SEK", "EUR"))
        assertEquals("EUR", resolveCurrencyToken(null, "EUR"))
        assertEquals("EUR", resolveCurrencyToken("", "EUR"))
        assertEquals("EUR", resolveCurrencyToken("   ", " EUR "))
        assertNull(resolveCurrencyToken(null, null))
        assertNull(resolveCurrencyToken("", ""))
    }

    @Test
    fun `without a manual choice or a sensor unit the locale decides`() {
        assertTrue(formatEnergyCost(12.34f, null, null, dutch).contains("€"))
        assertTrue(formatEnergyCost(12.34f, null, null, american).contains("$"))
        assertTrue(formatEnergyCost(12.34f, null, null, Locale.forLanguageTag("en-GB")).contains("£"))
    }

    @Test
    fun `the cost sensor's own unit is used when no currency was picked`() {
        // Home Assistant puts the instance currency in unit_of_measurement, normally an ISO code.
        assertTrue(formatEnergyCost(12.34f, null, "USD", dutch).contains("$"))
        // A bare symbol keeps the locale's placement instead of being treated as a code.
        assertEquals("kr 12,34", plainSpaces(formatEnergyCost(12.34f, null, "kr", dutch)))
    }

    @Test
    fun `a manual currency overrides what the sensor reports`() {
        assertTrue(formatEnergyCost(12.34f, "JPY", "EUR", dutch).contains("¥"))
        assertTrue(formatEnergyCost(12.34f, "eur", "USD", dutch).contains("€"))
    }

    @Test
    fun `numbers follow the locale's separators`() {
        assertTrue(formatEnergyCost(1234.5f, "EUR", null, dutch).contains("1.234,50"))
        assertTrue(formatEnergyCost(1234.5f, "USD", null, american).contains("1,234.50"))
    }

    @Test
    fun `currencies without minor units drop the decimals`() {
        assertEquals("1234", digits(formatEnergyCost(1234f, "JPY", null, dutch)))
        assertEquals("123450", digits(formatEnergyCost(1234.5f, "EUR", null, dutch)))
    }

    @Test
    fun `unknown or non-ISO tokens fall back to using the token as the symbol`() {
        assertNull(isoCurrencyOrNull("€"))
        assertNull(isoCurrencyOrNull("QQQ"))
        assertNull(isoCurrencyOrNull(null))
        assertNotNull(isoCurrencyOrNull("eur"))
        assertEquals("€", currencySymbolFor("€", dutch))
        assertEquals("QQQ 12,34", plainSpaces(formatEnergyCost(12.34f, "QQQ", null, dutch)))
    }

    @Test
    fun `a locale without a region still resolves a currency`() {
        val text = formatEnergyCost(12.34f, null, null, Locale.forLanguageTag("en"))
        assertTrue(text, text.none { it == '¤' })
    }

    @Test
    fun `the picker offers real currencies and skips fund and metal codes`() {
        val options = currencyOptions(american)
        val codes = options.map { it.code }
        assertTrue(codes.containsAll(listOf("EUR", "USD", "SEK", "JPY")))
        assertTrue(codes.none { it in setOf("XAU", "XAG", "XDR", "XPT") })
        // Sorted by the name shown, in the picker's locale.
        assertEquals(options.map { it.label.lowercase(american) }.sorted(), options.map { it.label.lowercase(american) })
    }
}
