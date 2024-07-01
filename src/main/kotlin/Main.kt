import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import viewmodel.ArchiveViewModel

@Composable
@Preview
fun App(
    viewModel: ArchiveViewModel = viewModel { ArchiveViewModel() }
) {
    val isParentDirectoryIncluded = viewModel.isParentDirectoryIncluded.collectAsState()
    val path = viewModel.path.collectAsState()
    val isArchiving = viewModel.isArchiving.collectAsState()
    val launcher = rememberDirectoryPickerLauncher(
        title = "Select a directory",
        initialDirectory = null,
        platformSettings = null
    ) { directory ->
        directory?.let { viewModel.setPath(it.file.toPath()) }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable { viewModel.toggleParentDirectory() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isParentDirectoryIncluded.value,
                    onCheckedChange = { viewModel.toggleParentDirectory() }
                )
                Text("Include directory in archive")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Column {
                    Text("Selected path: ${path.value?.toString() ?: "None"}")
                    Button(
                        onClick = { launcher.launch() },
                        content = { Text("Select directory") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = path.value != null && !isArchiving.value,
                onClick = { viewModel.onArchiveClick() },
                content = { Text("Archive") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isArchiving.value) {
                CircularProgressIndicator()
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
