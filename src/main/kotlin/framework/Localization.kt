package framework

import java.util.Locale
import java.util.ResourceBundle

object LocalizationManager {
    var locale: Locale = Locale.getDefault()
        private set
    val supportedLocales = listOf(Locale.ENGLISH, Locale.KOREA, Locale.JAPAN)
    private var bundle: ResourceBundle = ResourceBundle.getBundle("strings", locale)

    fun setLocale(locale: Locale) {
        this.locale = if (supportedLocales.contains(locale)) locale else Locale.ENGLISH
        bundle = ResourceBundle.getBundle("strings", this.locale)
    }

    fun getString(key: String): String {
        return bundle.getString(key)
    }
}

