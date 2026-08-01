package com.jimz011apps.hki7.data

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

const val SYSTEM_LANGUAGE_TAG = "system"

val supportedAppLanguageTags = listOf("en", "nl", "de", "fr", "es", "it", "tr")

/** Returns the explicit app override, or [SYSTEM_LANGUAGE_TAG] when Android follows the device. */
fun currentAppLanguage(context: Context): String {
    val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
    if (locales.isEmpty) return SYSTEM_LANGUAGE_TAG
    val language = locales[0].language
    return language.takeIf { it in supportedAppLanguageTags } ?: locales[0].toLanguageTag()
}

/** Applies a platform-managed per-app locale. Android recreates the Activity and persists it. */
fun setAppLanguage(context: Context, languageTag: String) {
    context.getSystemService(LocaleManager::class.java).applicationLocales =
        if (languageTag == SYSTEM_LANGUAGE_TAG || languageTag !in supportedAppLanguageTags) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(languageTag)
        }
}
