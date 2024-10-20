package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import framework.LocalizationState
import org.koin.compose.koinInject
import viewmodel.RarToZipViewModel
import viewmodel.TargetPickerType

@Composable
fun RarToZipView() {
    val localizationState: LocalizationState = koinInject()
    val viewModel: RarToZipViewModel = koinInject()
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
            titleText = localizationState.getString("title_rar_to_zip"),
            tooltip = localizationState.getString("tooltip_rar_to_zip")
        )

        ProcessesSection(viewModel)
    }
}