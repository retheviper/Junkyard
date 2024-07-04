package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import org.koin.compose.koinInject
import viewmodel.RarToZipViewModel

@Composable
fun RarToZipView() {
    val viewModel: RarToZipViewModel = koinInject()
    val path = viewModel.path.collectAsState()
    val isConverting = viewModel.isConverting.collectAsState()
    val convertedFiles = viewModel.processed.collectAsState()
    val failedFiles = viewModel.failed.collectAsState()

    val launcher = rememberDirectoryPickerLauncher(
        title = "Select a directory",
        initialDirectory = null,
        platformSettings = null
    ) { directory ->
        directory?.let { viewModel.setPath(it.file.toPath()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Converts RAR to ZIP",
            fontSize = 26.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        Text("Converted files: ${convertedFiles.value}")
        Text("Failed files: ${failedFiles.value}")

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column {
                Text("Selected path: ${path.value?.toString() ?: "None"}")

                Row {
                    Button(
                        onClick = { launcher.launch() }
                    ) {
                        Text("Select directory")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        enabled = path.value != null && !isConverting.value,
                        onClick = { viewModel.onConvertClick() }
                    ) {
                        Text("Convert")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isConverting.value) {
            CircularProgressIndicator()
        }
    }
}