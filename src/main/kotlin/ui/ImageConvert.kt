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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sksamuel.scrimage.format.Format
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import org.koin.compose.koinInject
import viewmodel.ImageConvertViewModel

@Composable
fun ImageConvertView() {
    val viewModel: ImageConvertViewModel = koinInject()
    val path = viewModel.path.collectAsState()
    val isConverting = viewModel.isConverting.collectAsState()
    val convertedFiles = viewModel.convertedFiles.collectAsState()
    val failedFiles = viewModel.failedFiles.collectAsState()

    var fromFormatExpanded by remember { mutableStateOf(false) }
    var fromFormat by remember { mutableStateOf(Format.JPEG) }
    var toFormatExpanded by remember { mutableStateOf(false) }
    var toFormat by remember { mutableStateOf(Format.WEBP) }

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
            text = "Convert images to different formats",
            fontSize = 26.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Divider()

        Text("Converted files: ${convertedFiles.value}")
        Text("Failed files: ${failedFiles.value}")

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            DropdownMenuBox(
                label = "From format:",
                selectedItem = fromFormat,
                items = Format.entries,
                onItemSelected = { fromFormat = it },
                expanded = fromFormatExpanded,
                onExpandedChange = { fromFormatExpanded = it }
            )

            Spacer(modifier = Modifier.width(16.dp))

            DropdownMenuBox(
                label = "To format:",
                selectedItem = toFormat,
                items = Format.entries.filterNot { it == fromFormat },
                onItemSelected = { toFormat = it },
                expanded = toFormatExpanded,
                onExpandedChange = { toFormatExpanded = it }
            )
        }

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
                        onClick = { viewModel.onConvertClick(fromFormat, toFormat) }
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

@Composable
fun DropdownMenuBox(
    label: String,
    selectedItem: Format,
    items: List<Format>,
    onItemSelected: (Format) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Column {
        Text(label)
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
                Icons.Filled.ArrowDropDown, "contentDescription",
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