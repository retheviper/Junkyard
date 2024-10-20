package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import framework.LocalizationState
import org.koin.compose.koinInject
import viewmodel.ResyncSubtitleViewModel
import viewmodel.TargetPickerType

@Composable
fun ResyncSubtitleView() {
    val localizationState: LocalizationState = koinInject()
    val viewModel: ResyncSubtitleViewModel = koinInject()
    val shiftMillis = viewModel.shiftMillis.collectAsState()
    val isDragOver = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .commonDragAndDrop(
                isDragOver = isDragOver,
                onDrop = { viewModel.handleDrop(it, TargetPickerType.FILE) }
            )
            .background(if (isDragOver.value) Color.LightGray else Color.Transparent)
    ) {
        TitleTextSection(
            titleText = localizationState.getString("title_shift_subtitle"),
            tooltip = localizationState.getString("tooltip_shift_subtitle")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            localizationState.getString("input_shift_milliseconds"),
            modifier = Modifier.padding(start = 16.dp)
        )

        NumberInputField(
            number = shiftMillis.value,
            onNumberChange = { value ->
                if (value == "-") {
                    viewModel.setShiftMillis(shiftMillis.value * -1)
                } else {
                    value.toIntOrNull()?.let { viewModel.setShiftMillis(it) }
                }
            },
            onIncrease = { viewModel.increaseShiftMillis() },
            onDecrease = { viewModel.decreaseShiftMillis() }
        )

        ProcessesSection(viewModel)
    }
}