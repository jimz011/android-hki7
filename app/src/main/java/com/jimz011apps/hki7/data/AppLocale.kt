package com.jimz011apps.hki7.data

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

const val SYSTEM_LANGUAGE_TAG = "system"

val supportedAppLanguageTags = listOf(
    "en", "nl", "de", "fr", "es", "it", "tr",
    "pt", "pt-BR", "es-419", "ja", "ko", "zh-CN", "zh-TW",
    "nb", "sv", "fi", "ar",
    "pl", "he", "ru", "th", "ro", "hu", "bg", "el", "cs", "sk",
    "lt", "da", "et", "lv", "hr",
    // Regional variants of a language already listed, named the way pt-BR and es-419 are.
    "de-CH", "de-AT", "es-MX"
)

/**
 * Java carries pre-1989 ISO codes for a few languages, so a Hebrew locale can come back as either
 * "iw" or "he" depending on which end of the platform produced it. The resource folder is
 * `values-iw` for the same historical reason; everything above the resource layer uses "he".
 */
private val legacyLanguageCodes = mapOf("iw" to "he", "in" to "id", "ji" to "yi")

private fun normalizeLanguageTag(tag: String): String {
    val language = tag.substringBefore('-')
    val modern = legacyLanguageCodes[language] ?: return tag
    return modern + tag.removePrefix(language)
}

// Android 13 introduced platform-managed per-app locales, and that remains the path this app takes
// wherever it's available. Android 12 has no such concept, so below API 33 the choice is stored
// here and applied to every context the app creates — see [withStoredAppLocale], called from
// HKI7Application and MainActivity's attachBaseContext.
private const val LOCALE_PREFS = "hki7_app_locale"
private const val LOCALE_KEY = "language_tag"

private fun localePrefs(context: Context) =
    context.getSharedPreferences(LOCALE_PREFS, Context.MODE_PRIVATE)

/** Returns the explicit app override, or [SYSTEM_LANGUAGE_TAG] when Android follows the device. */
fun currentAppLanguage(context: Context): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) platformAppLanguage(context)
    else storedAppLanguage(context)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun platformAppLanguage(context: Context): String {
    val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
    if (locales.isEmpty) return SYSTEM_LANGUAGE_TAG
    val tag = normalizeLanguageTag(locales[0].toLanguageTag())
    if (tag in supportedAppLanguageTags) return tag
    val language = normalizeLanguageTag(locales[0].language)
    return language.takeIf { it in supportedAppLanguageTags } ?: tag
}

/** Only supported tags are ever written, so anything else means "follow the device". */
private fun storedAppLanguage(context: Context): String =
    localePrefs(context).getString(LOCALE_KEY, null)
        ?.takeIf { it in supportedAppLanguageTags }
        ?: SYSTEM_LANGUAGE_TAG

/** Applies a per-app locale. On API 33+ Android persists it and recreates the Activity itself. */
fun setAppLanguage(context: Context, languageTag: String) {
    val tag = languageTag.takeIf { it != SYSTEM_LANGUAGE_TAG && it in supportedAppLanguageTags }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        setPlatformAppLanguage(context, tag)
        return
    }
    localePrefs(context).edit().putString(LOCALE_KEY, tag).apply()
    // Nothing recreates the Activity for us below API 33, and the running one still holds the old
    // configuration, so ask for it explicitly.
    context.findActivity()?.recreate()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun setPlatformAppLanguage(context: Context, tag: String?) {
    context.getSystemService(LocaleManager::class.java).applicationLocales =
        if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
}

/**
 * Wraps [this] so its resources resolve in the stored app language. A no-op on API 33+, where the
 * platform has already applied the locale before any app code runs, and on "follow the device".
 */
fun Context.withStoredAppLocale(): Context {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return this
    // Runs from attachBaseContext, where anything thrown means the app cannot start at all — fall
    // back to the device language rather than taking the process down for a locale preference.
    return runCatching {
        val tag = storedAppLanguage(this).takeIf { it != SYSTEM_LANGUAGE_TAG } ?: return this
        val config = Configuration(resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tag))
        createConfigurationContext(config)
    }.getOrDefault(this)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
