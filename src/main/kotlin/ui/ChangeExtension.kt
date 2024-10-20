package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import framework.LocalizationState
import org.koin.compose.koinInject
import viewmodel.ChangeExtensionViewModel
import viewmodel.TargetPickerType

@Composable
fun ChangeExtensionView() {
    val localizationState: LocalizationState = koinInject()
    val viewModel: ChangeExtensionViewModel = koinInject()
    val ignoreCase = viewModel.ignoreCase.collectAsState()
    val fromExtension = viewModel.fromExtension.collectAsState()
    val toExtension = viewModel.toExtension.collectAsState()
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
            titleText = localizationState.getString("title_change_file_extension"),
            tooltip = localizationState.getString("tooltip_change_file_extension")
        )

        CheckboxSection(
            onClick = { viewModel.toggleIgnoreCase() },
            checked = ignoreCase.value,
            text = localizationState.getString("ignore_case")
        )

        Row {
            LabeledTextField(
                label = "${localizationState.getString("from_extension")}:",
                value = fromExtension.value,
                onValueChange = { viewModel.setFromExtension(it) }
            )

            Spacer(modifier = Modifier.width(16.dp))

            LabeledTextField(
                label = "${localizationState.getString("to_extension")}:",
                value = toExtension.value,
                onValueChange = { viewModel.setToExtension(it) }
            )
        }

        ProcessesSection(viewModel)
    }
}