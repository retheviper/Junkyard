package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import org.koin.compose.koinInject
import viewmodel.ChangeExtensionViewModel

@Composable
fun ChangeExtensionView() {
    val viewModel: ChangeExtensionViewModel = koinInject()
    val path = viewModel.path.collectAsState()
    val isConverting = viewModel.isConverting.collectAsState()
    var ignoreCase by remember { mutableStateOf(true) }
    var fromExtension by remember { mutableStateOf("jpeg") }
    var toExtension by remember { mutableStateOf("jpg") }
    val processed = viewModel.processed.collectAsState()
    val failed = viewModel.failed.collectAsState()

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
            text = "Change file extension",
            fontSize = 26.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        Text("Changed Files: ${processed.value}")
        Text("Failed Files: ${failed.value}")

        Row(
            modifier = Modifier
                .clickable { ignoreCase = !ignoreCase }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = ignoreCase,
                onCheckedChange = { ignoreCase = !it }
            )
            Text("Ignore case")
        }


        Row {
            Column {
                Text("From extension:")
                TextField(
                    value = fromExtension,
                    onValueChange = { fromExtension = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp),
                    shape = RectangleShape,
                    modifier = Modifier.size(250.dp, 50.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text("To extension:")
                TextField(
                    value = toExtension,
                    onValueChange = { toExtension = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp),
                    modifier = Modifier.size(250.dp, 50.dp)
                )
            }
        }

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
                        onClick = {
                            viewModel.onConvertClick(
                                fromExtension,
                                toExtension,
                                ignoreCase
                            )
                        }
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