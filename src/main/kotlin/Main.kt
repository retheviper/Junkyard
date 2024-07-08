import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import framework.LocalizationManager
import framework.MyTheme
import framework.appModules
import java.awt.Dimension
import java.util.Locale
import org.koin.core.context.GlobalContext.startKoin
import ui.ArchiveView
import ui.ChangeExtensionView
import ui.CreateThumbnailView
import ui.ImageConvertView
import ui.RarToZipView

enum class Screen(val title: String, val icon: String) {
    Archive("Archive", "📦"),
    RarToZip("to Zip", "📚"),
    ChangeExtension("Extension", "🔄"),
    ImageConvert("Convert", "🖼️"),
    CreateThumbnail("Thumbnail", "📐"),
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val isSystemInDarkTheme = isSystemInDarkTheme()
    var darkMode by remember { mutableStateOf(isSystemInDarkTheme) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    MyTheme(
        darkTheme = darkMode,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                Column {
                    Screen.entries.forEach {
                        NavigationRailItem(
                            label = { Text(it.title) },
                            icon = { Text(it.icon) },
                            onClick = { navController.navigate(it.name) },
                            selected = navBackStackEntry?.destination?.route == it.name
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationRailItem(
                        label = { Text("Dark mode") },
                        icon = {
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { darkMode = it },
                                modifier = Modifier.padding(bottom = 50.dp)
                            )
                        },
                        onClick = { darkMode = !darkMode },
                        selected = false
                    )
                }
            }

            Surface {
                NavHost(
                    navController,
                    startDestination = Screen.Archive.name,
                    enterTransition = { slideInHorizontally() + fadeIn() },
                    exitTransition = { slideOutHorizontally() + fadeOut() }
                ) {
                    composable(Screen.Archive.name) { ArchiveView() }
                    composable(Screen.RarToZip.name) { RarToZipView() }
                    composable(Screen.ChangeExtension.name) { ChangeExtensionView() }
                    composable(Screen.ImageConvert.name) { ImageConvertView() }
                    composable(Screen.CreateThumbnail.name) { CreateThumbnailView() }
                }
            }
        }
    }
}

fun main() = application {
    startKoin {
        modules(appModules)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "🛠️Junkyard",
        onKeyEvent = { keyEvent ->
            (keyEvent.isMetaPressed && keyEvent.key == Key.W).also {
                if (it) exitApplication()
            }
        }
    ) {
        window.minimumSize = Dimension(800, 700)

        SettingsMenuBar()

        MainScreen()
    }
}

@Composable
private fun FrameWindowScope.SettingsMenuBar() {
    var locale by remember { mutableStateOf(Locale.getDefault()) }
    if (!LocalizationManager.supportedLocales.contains(locale)) {
        locale = Locale.ENGLISH
    }
    LocalizationManager.setLocale(locale)

    MenuBar {
        Menu(LocalizationManager.getString("settings")) {
            Menu(LocalizationManager.getString("language")) {
                CheckboxItem(
                    "English",
                    checked = locale == Locale.ENGLISH,
                    onCheckedChange = {
                        locale = Locale.ENGLISH
                    }
                )
                CheckboxItem(
                    "日本語",
                    checked = locale == Locale.JAPAN,
                    onCheckedChange = {
                        locale = Locale.JAPAN
                    }
                )
                CheckboxItem(
                    "한국어",
                    checked = locale == Locale.KOREA,
                    onCheckedChange = {
                        locale = Locale.KOREA
                    }
                )
            }
        }
    }
}
