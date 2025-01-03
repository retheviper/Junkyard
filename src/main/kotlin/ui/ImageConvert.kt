package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sksamuel.scrimage.format.Format
import framework.LocalizationState
import org.koin.compose.koinInject
import viewmodel.ImageConvertViewModel
import viewmodel.TargetPickerType

@Composable
fun ImageConvertView() {
    val localizationState: LocalizationState = koinInject()
    val viewModel: ImageConvertViewModel = koinInject()
    val fromFormat = viewModel.fromFormat.collectAsState()
    val toFormat = viewModel.toFormat.collectAsState()
    var fromFormatExpanded by remember { mutableStateOf(false) }
    var toFormatExpanded by remember { mutableStateOf(false) }
    var selectableFormats by remember { mutableStateOf(Format.entries.filterNot { format -> format == fromFormat.value }) }
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
            titleText = localizationState.getString("title_convert_image_format"),
            tooltip = localizationState.getString("tooltip_convert_image_format")
        )

        CheckboxSection(
            onClick = { viewModel.toggleIncludeArchiveFiles() },
            checked = viewModel.includeArchiveFiles.collectAsState().value,
            text = localizationState.getString("include_archive_files")
        )

        Row {
            DropdownMenuBox(
                label = "${localizationState.getString("from_format")}:",
                selectedItem = fromFormat.value,
                items = Format.entries,
                onItemSelected = {
                    viewModel.setFromFormat(it)
                    selectableFormats = Format.entries.filterNot { format -> format == it }
                    if (toFormat.value == it) {
                        viewModel.setToFormat(selectableFormats.first())
                    }
                },
                expanded = fromFormatExpanded,
                onExpandedChange = { fromFormatExpanded = it }
            )

            Spacer(modifier = Modifier.width(16.dp))

            DropdownMenuBox(
                label = "${localizationState.getString("to_format")}:",
                selectedItem = toFormat.value,
                items = selectableFormats,
                onItemSelected = { viewModel.setToFormat(it) },
                expanded = toFormatExpanded,
                onExpandedChange = { toFormatExpanded = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProcessesSection(viewModel)
    }
}