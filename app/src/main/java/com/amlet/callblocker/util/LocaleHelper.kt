package com.amlet.callblocker.util

import android.content.Context
import android.content.res.Configuration
import com.amlet.callblocker.data.prefs.AppPreferences
import java.util.Locale

object LocaleHelper {

    /**
     * Applies the saved language preference to the given context.
     * Call this in Activity.attachBaseContext and whenever the user changes the language.
     */
    fun applyLocale(context: Context, languageCode: String) {
        val locale = when (languageCode) {
            AppPreferences.LANG_EN -> Locale.ENGLISH
            AppPreferences.LANG_IT -> Locale.ITALIAN
            else -> Locale.getDefault() // "system" — use device locale
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    /** Returns a Context with the saved locale applied — use in attachBaseContext. */
    fun wrapContext(context: Context): Context {
        val prefs = AppPreferences(context)
        val languageCode = prefs.appLanguage
        if (languageCode == AppPreferences.LANG_SYSTEM) return context

        val locale = when (languageCode) {
            AppPreferences.LANG_EN -> Locale.ENGLISH
            AppPreferences.LANG_IT -> Locale.ITALIAN
            else -> return context
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
