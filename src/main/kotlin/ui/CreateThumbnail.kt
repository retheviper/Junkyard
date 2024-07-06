package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sksamuel.scrimage.format.Format
import org.koin.compose.koinInject
import viewmodel.CreateThumbnailOption
import viewmodel.CreateThumbnailViewModel
import viewmodel.ImageOutputFormat

@Composable
fun CreateThumbnailView() {
    val viewModel: CreateThumbnailViewModel = koinInject()
    val targetFormats = viewModel.targetFormats.collectAsState()
    val outputFormat = viewModel.imageOutputFormat.collectAsState()
    val selectedOption = viewModel.option.collectAsState()
    val width = viewModel.width.collectAsState()
    val height = viewModel.height.collectAsState()
    val ratio = viewModel.ratio.collectAsState()
    var outputFormatExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Create thumbnails from images",
            fontSize = 26.sp
        )

        Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        Text("Target formats:")

        Row {
            Format.entries.forEach { format ->
                CheckboxSection(
                    onClick = {
                        if (targetFormats.value.contains(format)) {
                            viewModel.removeTargetFormat(format)
                        } else {
                            viewModel.addTargetFormat(format)
                        }
                    },
                    checked = targetFormats.value.contains(format),
                    text = format.name
                )
            }
        }

        DropdownMenuBox(
            label = "Output format:",
            selectedItem = outputFormat.value,
            items = ImageOutputFormat.entries,
            onItemSelected = { viewModel.setImageOutputFormat(it) },
            expanded = outputFormatExpanded,
            onExpandedChange = { outputFormatExpanded = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Resize options:")

        Row {
            CreateThumbnailOption.entries.forEach { option ->
                Row(
                    modifier = Modifier.clickable { viewModel.setOption(option) }
                ) {
                    RadioButton(
                        selected = selectedOption.value == option,
                        onClick = { viewModel.setOption(option) }
                    )
                    Text(
                        text = option.description,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        when (selectedOption.value) {
            CreateThumbnailOption.FIXED_SIZE -> {
                Row {
                    LabeledTextField(
                        label = "width:",
                        value = width.value.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { width ->
                                viewModel.setWidth(width)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    LabeledTextField(
                        label = "height:",
                        value = height.value.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { height ->
                                viewModel.setHeight(height)
                            }
                        }
                    )
                }
            }

            CreateThumbnailOption.ASPECT_RATIO -> {
                Row {
                    LabeledTextField(
                        label = "width:",
                        value = width.value.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { width ->
                                viewModel.setWidth(width)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    LabeledTextField(
                        label = "ratio:",
                        value = ratio.value.toString(),
                        onValueChange = {
                            it.toDoubleOrNull()?.let { ratio ->
                                if (ratio > 0 && ratio <= 100.0) {
                                    viewModel.setRatio(ratio)
                                }
                            }
                        }
                    )
                }
            }

            CreateThumbnailOption.RATIO -> {
                LabeledTextField(
                    label = "ratio:",
                    value = ratio.value.toString(),
                    onValueChange = {
                        it.toDoubleOrNull()?.let { ratio ->
                            if (ratio > 0 && ratio <= 100.0) {
                                viewModel.setRatio(ratio)
                            }
                        }
                    }
                )
            }
        }

        ProcessesSection(viewModel)
    }
}