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
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import framework.LocalizationManager
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import viewmodel.ProcessViewModel

@Composable
fun ProcessesSection(viewModel: ProcessViewModel) {
    val path = viewModel.path.collectAsState()
    val isProcessing = viewModel.isProcessing.collectAsState()
    val processed = viewModel.processed.collectAsState()
    val failed = viewModel.failed.collectAsState()
    val launcher = rememberDirectoryPickerLauncher(
        title = LocalizationManager.getString("select_directory"),
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
            text = "${LocalizationManager.getString("success")}: ${processed.value}",
            fontSize = 16.sp
        )
        Text(
            text = "${LocalizationManager.getString("failed")}: ${failed.value}",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column {
                Text("${LocalizationManager.getString("selected_path")}: ${path.value ?: LocalizationManager.getString("none")}")
                Row {
                    Button(
                        onClick = { launcher.launch() }
                    ) {
                        Text(LocalizationManager.getString("select_directory"))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        enabled = path.value != null && !isProcessing.value,
                        onClick = viewModel::onProcessClick
                    ) {
                        Text(LocalizationManager.getString("process"))
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