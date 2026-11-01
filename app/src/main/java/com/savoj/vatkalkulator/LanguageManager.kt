package com.savoj.vatkalkulator

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {

    /** Українська на українському телефоні, на всіх інших польська. */
    fun wrap(base: Context): Context {
        val phone = base.resources.configuration.locales[0].language
        val language = if (phone == "uk") "uk" else "pl"
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}