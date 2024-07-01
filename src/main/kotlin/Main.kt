import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ui.ArchiveView
import ui.RarToZipView

enum class Screen(val title: String) {
    Archive("Archive"),
    RarToZip("Rar to Zip")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.Archive.name) },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(Screen.Archive.title)
                }
                Button(
                    onClick = { navController.navigate(Screen.RarToZip.name) },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(Screen.RarToZip.title)
                }
            }

            NavHost(navController, startDestination = Screen.Archive.name) {
                composable(Screen.Archive.name) { ArchiveView() }
                composable(Screen.RarToZip.name) { RarToZipView() }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MainScreen()
    }
}
