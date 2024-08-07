package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Text(
            text = "${localizationState.getString("success")}: ${processed.value}",
            fontSize = 16.sp
        )
        Text(
            text = "${localizationState.getString("failed")}: ${failed.value}",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column {
                Text("${localizationState.getString("selected_path")}: ${path.value ?: localizationState.getString("none")}")
                Row {
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
                        enabled = path.value != null && !isProcessing.value,
                        onClick = viewModel::onProcessClick
                    ) {
                        Text(localizationState.getString("process"))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isProcessing.value) {
            CircularProgressIndicator()
        }
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
    Column {
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
            .clickable { onClick() }
            .padding(8.dp),
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
    Column {
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

@Composable
fun TitleTextSection(text: String) {
    Text(
        text,
        fontSize = 26.sp
    )

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
        }
    )
}
