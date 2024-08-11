package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import framework.LocalizationState
import org.koin.compose.koinInject
import viewmodel.ResyncSubtitleViewModel

@Composable
fun ResyncSubtitleView() {
    val localizationState: LocalizationState = koinInject()
    val viewModel: ResyncSubtitleViewModel = koinInject()
    val shiftMillis = viewModel.shiftMillis.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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