package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import viewmodel.ArchiveViewModel

@Composable
fun ArchiveView() {
    val viewModel: ArchiveViewModel = koinInject()
    val isParentDirectoryIncluded = viewModel.isParentDirectoryIncluded.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Archive subdirectories",
            fontSize = 26.sp
        )

        Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        CheckboxSection(
            onClick = { viewModel.toggleParentDirectory() },
            checked = isParentDirectoryIncluded.value,
            text = "Include directory in archive"
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProcessesSection(viewModel)
    }
}