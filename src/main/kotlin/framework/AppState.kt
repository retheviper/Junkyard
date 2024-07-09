package framework

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.Locale
import java.util.ResourceBundle

class LocalizationState {
    private var resourceBundle: MutableState<ResourceBundle>
    private val supportedLocales = listOf(Locale.ENGLISH, Locale.JAPAN, Locale.KOREA)

    init {
        val locale = Locale.getDefault().let { defaultLocale ->
            if (supportedLocales.contains(defaultLocale)) defaultLocale else Locale.ENGLISH
        }
        resourceBundle = mutableStateOf(ResourceBundle.getBundle("strings", locale))
    }

    val currentLocale: Locale
        get() = resourceBundle.value.locale

    fun setLocale(locale: Locale) {
        resourceBundle.value = ResourceBundle.getBundle("strings", locale)
    }

    fun getString(key: String): String {
        return resourceBundle.value.getString(key)
    }
}

@Composable
fun rememberLocalizationState(): LocalizationState {
    return remember { LocalizationState() }
}