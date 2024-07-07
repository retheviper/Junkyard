package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import framework.LocalizationManager
import org.koin.compose.koinInject
import viewmodel.ChangeExtensionViewModel

@Composable
fun ChangeExtensionView() {
    val viewModel: ChangeExtensionViewModel = koinInject()
    val ignoreCase = viewModel.ignoreCase.collectAsState()
    val fromExtension = viewModel.fromExtension.collectAsState()
    val toExtension = viewModel.toExtension.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TitleTextSection(LocalizationManager.getString("title_change_file_extension"))

        CheckboxSection(
            onClick = { viewModel.toggleIgnoreCase() },
            checked = ignoreCase.value,
            text = LocalizationManager.getString("ignore_case")
        )

        Row {
            LabeledTextField(
                label = "${LocalizationManager.getString("from_extension")}:",
                value = fromExtension.value,
                onValueChange = { viewModel.setFromExtension(it) }
            )

            Spacer(modifier = Modifier.width(16.dp))

            LabeledTextField(
                label = "${LocalizationManager.getString("to_extension")}:",
                value = toExtension.value,
                onValueChange = { viewModel.setToExtension(it) }
            )
        }

        ProcessesSection(viewModel)
    }
}