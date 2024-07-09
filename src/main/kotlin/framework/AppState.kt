package framework

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.Locale
import java.util.ResourceBundle

class LocalizationState {
    private val resourceBundle: MutableState<ResourceBundle> = mutableStateOf(ResourceBundle.getBundle("strings"))
    private val supportedLocales = listOf(Locale.ENGLISH, Locale.JAPAN, Locale.KOREA)

    val currentLocale: Locale
        get() = resourceBundle.value.locale

    fun setLocale(newLocale: Locale) {
        val locale = if (supportedLocales.contains(newLocale)) newLocale else Locale.ENGLISH
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