package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sksamuel.scrimage.format.Format
import org.koin.compose.koinInject
import viewmodel.ImageConvertViewModel

@Composable
fun ImageConvertView() {
    val viewModel: ImageConvertViewModel = koinInject()
    val fromFormat = viewModel.fromFormat.collectAsState()
    val toFormat = viewModel.toFormat.collectAsState()
    var fromFormatExpanded by remember { mutableStateOf(false) }
    var toFormatExpanded by remember { mutableStateOf(false) }
    var selectableFormats by remember { mutableStateOf(Format.entries.filterNot { format -> format == fromFormat.value }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Convert images to different formats",
            fontSize = 26.sp
        )

        Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        Row {
            DropdownMenuBox(
                label = "From format:",
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
                label = "To format:",
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