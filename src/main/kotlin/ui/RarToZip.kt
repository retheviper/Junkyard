package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import viewmodel.RarToZipViewModel

@Composable
fun RarToZipView(
    viewModel: RarToZipViewModel = viewModel { RarToZipViewModel() }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val path = viewModel.path.collectAsState()
        val isConverting = viewModel.isConverting.collectAsState()
        val convertedFiles = viewModel.convertedFiles.collectAsState()
        val failedFiles = viewModel.failedFiles.collectAsState()

        val launcher = rememberDirectoryPickerLauncher(
            title = "Select a directory",
            initialDirectory = null,
            platformSettings = null
        ) { directory ->
            directory?.let { viewModel.setPath(it.file.toPath()) }
        }
        Text("Convert RAR files to ZIP")

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
                Button(
                    onClick = { launcher.launch() },
                    content = { Text("Select directory") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = path.value != null && !isConverting.value,
            onClick = { viewModel.onConvertClick() },
            content = { Text("Convert") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isConverting.value) {
            CircularProgressIndicator()
        }
    }
}