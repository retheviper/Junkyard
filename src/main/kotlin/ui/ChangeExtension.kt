package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Text(
            text = "Change file extension",
            fontSize = 26.sp
        )

        Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        CheckboxSection(
            onClick = { viewModel.toggleIgnoreCase() },
            checked = ignoreCase.value,
            text = "Ignore case"
        )

        Row {
            LabeledTextField(
                label = "From extension:",
                value = fromExtension.value,
                onValueChange = { viewModel.setFromExtension(it) }
            )

            Spacer(modifier = Modifier.width(16.dp))

            LabeledTextField(
                label = "To extension:",
                value = toExtension.value,
                onValueChange = { viewModel.setToExtension(it) }
            )
        }

        ProcessesSection(viewModel)
    }
}