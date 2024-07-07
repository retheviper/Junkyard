package ui

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
import androidx.compose.ui.unit.dp
import com.sksamuel.scrimage.format.Format
import framework.LocalizationManager
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
        TitleTextSection(LocalizationManager.getString("title_convert_image_format"))

        Row {
            DropdownMenuBox(
                label = "${LocalizationManager.getString("from_format")}t:",
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
                label = "${LocalizationManager.getString("to_format")}}:",
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