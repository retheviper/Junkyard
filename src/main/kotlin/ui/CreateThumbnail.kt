package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sksamuel.scrimage.format.Format
import framework.LocalizationState
import org.koin.compose.koinInject
import viewmodel.CreateThumbnailOption
import viewmodel.CreateThumbnailViewModel
import viewmodel.ImageOutputFormat
import viewmodel.TargetPickerType

@Composable
fun CreateThumbnailView() {
    val localizationState: LocalizationState = koinInject()
    val viewModel: CreateThumbnailViewModel = koinInject()
    val targetFormats = viewModel.targetFormats.collectAsState()
    val outputFormat = viewModel.imageOutputFormat.collectAsState()
    val selectedOption = viewModel.option.collectAsState()
    var outputFormatExpanded by remember { mutableStateOf(false) }
    val isDragOver = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .commonDragAndDrop(
                isDragOver = isDragOver,
                onDrop = { viewModel.handleDrop(it, TargetPickerType.DIRECTORY) }
            )
            .background(if (isDragOver.value) Color.LightGray else Color.Transparent)
    ) {
        TitleTextSection(
            titleText = localizationState.getString("title_create_thumbnail"),
            tooltip = localizationState.getString("tooltip_create_thumbnail")
        )

        Text(
            "${localizationState.getString("target_format")}:",
            modifier = Modifier.padding(start = 16.dp)
        )

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
            label = "${localizationState.getString("output_format")}:",
            selectedItem = outputFormat.value,
            items = ImageOutputFormat.entries,
            onItemSelected = { viewModel.setImageOutputFormat(it) },
            expanded = outputFormatExpanded,
            onExpandedChange = { outputFormatExpanded = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "${localizationState.getString("resize_options")}:",
            modifier = Modifier.padding(start = 16.dp)
        )

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
                        text = localizationState.getString(option.name.lowercase()),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        when (selectedOption.value) {
            CreateThumbnailOption.FIXED_SIZE -> {
                Row {
                    WidthInput(viewModel)

                    Spacer(modifier = Modifier.width(16.dp))

                    LabeledTextField(
                        label = "${localizationState.getString("height")}:",
                        value = viewModel.height.collectAsState().value.toString(),
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
                    WidthInput(viewModel)

                    Spacer(modifier = Modifier.width(16.dp))

                    RatioInput(viewModel)
                }
            }

            CreateThumbnailOption.RATIO -> {
                RatioInput(viewModel)
            }
        }

        ProcessesSection(viewModel)
    }
}

@Composable
private fun WidthInput(viewModel: CreateThumbnailViewModel) {
    val localizationState: LocalizationState = koinInject()

    LabeledTextField(
        label = "${localizationState.getString("width")}:",
        value = viewModel.width.collectAsState().value.toString(),
        onValueChange = {
            it.toIntOrNull()?.let { width ->
                viewModel.setWidth(width)
            }
        }
    )
}

@Composable
private fun RatioInput(viewModel: CreateThumbnailViewModel) {
    val localizationState: LocalizationState = koinInject()

    LabeledTextField(
        label = "${localizationState.getString("ratio")}:",
        value = viewModel.ratio.collectAsState().value.toString(),
        onValueChange = {
            it.toDoubleOrNull()?.let { ratio ->
                if (ratio > 0 && ratio <= 100.0) {
                    viewModel.setRatio(ratio)
                }
            }
        }
    )
}
