package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// Cost values on the Energy view used to be hardcoded to euros. They now follow, in order: the
// currency picked manually in Energy settings, the cost sensor's own unit_of_measurement (Home
// Assistant puts the instance currency there, normally an ISO code such as "EUR"), and finally the
// device's region. Placement, spacing, and separators come from the locale, so the same amount
// renders as "€ 12,34" in Dutch and "$12.34" in US English.

/** The placeholder NumberFormat renders when a locale has no currency of its own. */
private const val GENERIC_CURRENCY_SIGN = "¤"

/** Amount used for the previews in the currency picker. */
private const val CURRENCY_SAMPLE_AMOUNT = 12.34

/** ISO-4217 currency for [token], or null when it is a bare symbol like "€" or "kr". */
internal fun isoCurrencyOrNull(token: String?): Currency? {
    val code = token?.trim()?.takeIf { it.length == 3 && it.all(Char::isLetter) } ?: return null
    return runCatching { Currency.getInstance(code.uppercase(Locale.ROOT)) }.getOrNull()
}

/** The locale's own currency, falling back to the system default for a locale without a region. */
private fun localeCurrency(locale: Locale): Currency? =
    runCatching { Currency.getInstance(locale) }.getOrNull()
        ?: runCatching { Currency.getInstance(Locale.getDefault()) }.getOrNull()

/** The manual choice from Energy settings, else the cost sensor's unit, else null for the locale. */
internal fun resolveCurrencyToken(manual: String?, sensorUnit: String?): String? =
    manual?.trim()?.ifEmpty { null } ?: sensorUnit?.trim()?.ifEmpty { null }

private fun currencyFormat(token: String?, locale: Locale): NumberFormat {
    val format = NumberFormat.getCurrencyInstance(locale)
    val decimal = format as? DecimalFormat
    val iso = isoCurrencyOrNull(token)
    if (iso != null) {
        format.currency = iso
        // setCurrency deliberately leaves the digit count alone, so currencies without minor units
        // (JPY, HUF, …) would keep the locale's two decimals without this.
        val digits = iso.defaultFractionDigits.coerceAtLeast(0)
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
    } else if (!token.isNullOrBlank() && decimal != null) {
        // A bare symbol: swapping it into the locale's own pattern keeps that locale's placement.
        decimal.decimalFormatSymbols = decimal.decimalFormatSymbols.also { it.currencySymbol = token.trim() }
    } else {
        val symbol = decimal?.decimalFormatSymbols?.currencySymbol
        if (symbol.isNullOrBlank() || symbol == GENERIC_CURRENCY_SIGN) {
            localeCurrency(locale)?.let { format.currency = it }
        }
    }
    return format
}

/** Formats a cost value, e.g. "€ 12,34" (nl-NL) or "$12.34" (en-US). */
internal fun formatEnergyCost(amount: Float, manual: String?, sensorUnit: String?, locale: Locale): String {
    val token = resolveCurrencyToken(manual, sensorUnit)
    return runCatching { currencyFormat(token, locale).format(amount.toDouble()) }
        .getOrElse { "%s %.2f".format(locale, currencySymbolFor(token, locale), amount) }
}

/** The symbol [token] resolves to, for the settings previews. */
internal fun currencySymbolFor(token: String?, locale: Locale): String {
    isoCurrencyOrNull(token)?.let { return it.getSymbol(locale) }
    if (!token.isNullOrBlank()) return token.trim()
    return localeCurrency(locale)?.getSymbol(locale).orEmpty()
}

internal data class CurrencyOption(val code: String, val label: String, val symbol: String)

internal fun currencyOptions(locale: Locale): List<CurrencyOption> =
    Currency.getAvailableCurrencies()
        // Funds and metal codes (XAU, XDR, …) report no minor unit and are never a utility tariff.
        .filter { it.defaultFractionDigits >= 0 }
        .map { CurrencyOption(it.currencyCode, it.getDisplayName(locale), it.getSymbol(locale)) }
        .sortedBy { it.label.lowercase(locale) }

/** Picker for [com.jimz011apps.hki7.data.HKIEnergyConfig.currencyCode]; null selects automatic. */
@Composable
internal fun EnergyCurrencyPickerDialog(
    current: String?,
    autoSensorUnit: String?,
    onDismiss: () -> Unit,
    onSelected: (String?) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    var query by remember { mutableStateOf("") }
    val options = remember(locale) { currencyOptions(locale) }
    val filtered = remember(options, query) {
        if (query.isBlank()) options
        else options.filter {
            it.code.contains(query, ignoreCase = true) ||
                it.label.contains(query, ignoreCase = true) ||
                it.symbol.contains(query, ignoreCase = true)
        }
    }
    val selectedCode = current?.trim().orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.energy_extra_currency)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.energy_extra_currency_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalHKIAppColors.current.onMuted
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.energy_extra_currency_search)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val listState = rememberLazyListState()
                LazyColumn(Modifier.heightIn(max = 340.dp).fadingEdges(listState), state = listState) {
                    if (query.isBlank()) {
                        item {
                            CurrencyOptionRow(
                                title = stringResource(R.string.energy_extra_currency_automatic),
                                subtitle = stringResource(R.string.energy_extra_currency_automatic_subtitle),
                                preview = formatEnergyCost(
                                    CURRENCY_SAMPLE_AMOUNT.toFloat(), null, autoSensorUnit, locale
                                ),
                                isSelected = selectedCode.isEmpty(),
                                onClick = { onSelected(null) }
                            )
                        }
                    }
                    items(filtered.size) { index ->
                        val option = filtered[index]
                        CurrencyOptionRow(
                            title = option.label,
                            subtitle = option.code,
                            preview = formatEnergyCost(
                                CURRENCY_SAMPLE_AMOUNT.toFloat(), option.code, null, locale
                            ),
                            isSelected = selectedCode.equals(option.code, ignoreCase = true),
                            onClick = { onSelected(option.code) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close_bbfa773)) } }
    )
}

@Composable
private fun CurrencyOptionRow(
    title: String,
    subtitle: String,
    preview: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else appColors.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            preview, style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else appColors.onMuted,
            maxLines = 1
        )
    }
    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.06f))
}
