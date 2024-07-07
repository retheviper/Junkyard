package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import framework.LocalizationManager
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
        TitleTextSection(LocalizationManager.getString("title_archive_subdirectories"))

        CheckboxSection(
            onClick = { viewModel.toggleParentDirectory() },
            checked = isParentDirectoryIncluded.value,
            text = LocalizationManager.getString("include_parent_directory")
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProcessesSection(viewModel)
    }
}