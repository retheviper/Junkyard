package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
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

        Row(
            modifier = Modifier
                .clickable { viewModel.toggleIgnoreCase() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = ignoreCase.value,
                onCheckedChange = { viewModel.toggleIgnoreCase() }
            )
            Text("Ignore case")
        }


        Row {
            ExtensionTextField(
                label = "From extension:",
                value = fromExtension.value,
                onValueChange = { viewModel.setFromExtension(it) }
            )

            Spacer(modifier = Modifier.width(16.dp))

            ExtensionTextField(
                label = "To extension:",
                value = toExtension.value,
                onValueChange = { viewModel.setToExtension(it) }
            )
        }

        ProcessesSection(viewModel)
    }
}

@Composable
fun ExtensionTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(label)
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp),
            shape = RectangleShape,
            modifier = Modifier.size(250.dp, 50.dp)
        )
    }
}