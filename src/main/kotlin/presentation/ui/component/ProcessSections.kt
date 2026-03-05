package presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import org.koin.compose.koinInject
import presentation.i18n.LocalizationState
import presentation.viewmodel.ProcessViewModel
import presentation.viewmodel.TargetPickerType

@Composable
fun ProcessesSection(viewModel: ProcessViewModel) {
    val localizationState: LocalizationState = koinInject()
    val path = viewModel.path.collectAsState()
    val isProcessing = viewModel.isProcessing.collectAsState()
    val processed = viewModel.processed.collectAsState()
    val failed = viewModel.failed.collectAsState()
    val progress = viewModel.progress.collectAsState()
    val currentFile = viewModel.currentFile.collectAsState()
    val showStatusDialog = remember { mutableStateOf(false) }

    val directoryLauncher = rememberDirectoryPickerLauncher(
        title = localizationState.getString("select_directory"),
        initialDirectory = null,
        platformSettings = null
    ) { directory ->
        directory?.let { viewModel.setPath(it.file.toPath()) }
    }

    val fileLauncher = rememberFilePickerLauncher(
        title = localizationState.getString("select_file"),
        type = PickerType.File(extensions = viewModel.targetExtensions),
        initialDirectory = null,
        platformSettings = null
    ) { file ->
        file?.let { viewModel.setPath(it.file.toPath()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text("${localizationState.getString("selected_path")}: ${path.value ?: localizationState.getString("none")}")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isProcessing.value) {
                        CircularProgressIndicator()
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    when (viewModel.targetPickerType) {
                        TargetPickerType.DIRECTORY -> {
                            Button(onClick = { directoryLauncher.launch() }) {
                                Text(localizationState.getString("select_directory"))
                            }
                        }

                        TargetPickerType.FILE -> {
                            Button(onClick = { fileLauncher.launch() }) {
                                Text(localizationState.getString("select_file"))
                            }
                        }

                        TargetPickerType.BOTH -> {
                            Button(onClick = { directoryLauncher.launch() }) {
                                Text(localizationState.getString("select_directory"))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { fileLauncher.launch() }) {
                                Text(localizationState.getString("select_file"))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        enabled = path.value != null,
                        onClick = {
                            if (isProcessing.value) {
                                viewModel.cancel()
                            } else {
                                viewModel.onProcessClick()
                                showStatusDialog.value = true
                            }
                        },
                        colors = if (isProcessing.value) {
                            ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                        } else {
                            ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                        }
                    ) {
                        if (isProcessing.value) {
                            Text(localizationState.getString("cancel"))
                        } else {
                            Text(localizationState.getString("process"))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { showStatusDialog.value = true },
                        enabled = isProcessing.value || processed.value > 0 || failed.value > 0
                    ) {
                        Text("Show Status")
                    }
                }
            }
        }

        ProcessStatusDialog(
            localizationState = localizationState,
            isVisible = showStatusDialog.value,
            onDismiss = { showStatusDialog.value = false },
            processed = processed.value,
            failed = failed.value,
            currentFile = currentFile.value.takeIf { it.isNotEmpty() },
            progress = progress.value
        )
    }
}

@Composable
fun ProcessStatusDialog(
    localizationState: LocalizationState,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    processed: Int,
    failed: Int,
    currentFile: String?,
    progress: Float
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Text(text = "Processing Status")
            },
            text = {
                Column {
                    Text(
                        text = "${localizationState.getString("success")}: $processed",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${localizationState.getString("failed")}: $failed",
                        fontSize = 16.sp
                    )
                    currentFile?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${localizationState.getString("current_file")}: $currentFile",
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(progress = progress)
                }
            },
            confirmButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Close")
                }
            }
        )
    }
}
