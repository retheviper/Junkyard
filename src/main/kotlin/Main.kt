import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import framework.MyTheme
import framework.appModules
import java.awt.Dimension
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
                            selected = navController.currentDestination?.route == it.name
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

@Composable
private fun MenuButton(onClick: () -> Unit, icon: String, expandedText: String, sidebarWidth: Dp) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        if (sidebarWidth >= 225.dp) Text("$icon $expandedText") else Text(icon)
    }
}

fun main() = application {
    startKoin {
        modules(appModules)
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "🛠️Junkyard",
    ) {
        window.minimumSize = Dimension(800, 700)
        MainScreen()
    }
}
