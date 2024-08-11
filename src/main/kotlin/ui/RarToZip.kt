package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import framework.LocalizationState
import org.koin.compose.koinInject
import viewmodel.RarToZipViewModel

@Composable
fun RarToZipView() {
    val localizationState: LocalizationState = koinInject()
    val viewModel: RarToZipViewModel = koinInject()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TitleTextSection(
            titleText = localizationState.getString("title_rar_to_zip"),
            tooltip = localizationState.getString("tooltip_rar_to_zip")
        )

        ProcessesSection(viewModel)
    }
}