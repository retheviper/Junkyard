package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import framework.LocalizationState
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import org.koin.compose.koinInject
import viewmodel.ProcessViewModel
import viewmodel.TargetPickerType

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

    val launcher = when (viewModel.targetPickerType) {
        TargetPickerType.DIRECTORY -> rememberDirectoryPickerLauncher(
            title = localizationState.getString("select_directory"),
            initialDirectory = null,
            platformSettings = null
        ) { directory ->
            directory?.let { viewModel.setPath(it.file.toPath()) }
        }

        TargetPickerType.FILE -> rememberFilePickerLauncher(
            title = localizationState.getString("select_file"),
            type = PickerType.File(extensions = viewModel.targetExtensions),
            initialDirectory = null,
            platformSettings = null
        ) { file ->
            file?.let { viewModel.setPath(it.file.toPath()) }
        }
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

                    Button(
                        onClick = { launcher.launch() }
                    ) {
                        Text(
                            when (viewModel.targetPickerType) {
                                TargetPickerType.DIRECTORY -> localizationState.getString("select_directory")
                                TargetPickerType.FILE -> localizationState.getString("select_file")
                            }
                        )
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
fun <T : Enum<T>> DropdownMenuBox(
    label: String,
    selectedItem: T,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(label)

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .size(250.dp, 50.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(4.dp))
                .clickable { onExpandedChange(!expanded) }
        ) {
            Text(
                text = selectedItem.name,
                modifier = Modifier.padding(start = 10.dp)
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                "contentDescription",
                Modifier.align(Alignment.CenterEnd)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                items.forEach {
                    DropdownMenuItem(
                        onClick = {
                            onItemSelected(it)
                            onExpandedChange(false)
                        }
                    ) {
                        Text(it.name)
                    }
                }
            }
        }
    }
}


@Composable
fun CheckboxSection(
    onClick: () -> Unit,
    checked: Boolean,
    text: String
) {
    Row(
        modifier = Modifier
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onClick() }
        )
        Text(text)
    }
}

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(label)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp),
            shape = RectangleShape,
            modifier = Modifier.size(250.dp, 50.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TitleTextSection(titleText: String, tooltip: String) {
    Row(
        modifier = Modifier.padding(8.dp),
    ) {
        Text(
            titleText,
            fontSize = 26.sp
        )

        TooltipArea(
            tooltip = {
                Surface(
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colors.secondary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tooltip,
                        color = MaterialTheme.colors.onSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))
}

@Composable
fun NumberInputField(
    number: Int,
    onNumberChange: (String) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    OutlinedTextField(
        value = number.toString(),
        onValueChange = onNumberChange,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        singleLine = true,
        trailingIcon = {
            Column {
                IconButton(onClick = onIncrease, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                }
                IconButton(onClick = onDecrease, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                }
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.commonDragAndDrop(
    isDragOver: MutableState<Boolean>,
    onDrop: (DragAndDropEvent) -> Boolean
): Modifier {
    val target = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isDragOver.value = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragOver.value = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragOver.value = false
                return onDrop(event)
            }
        }
    }

    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { true },
        target = target
    )
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