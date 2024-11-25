package ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sksamuel.scrimage.format.Format
import framework.LocalizationState
import org.koin.compose.koinInject
import viewmodel.TargetPickerType
import viewmodel.VideoCodec
import viewmodel.VideoConvertViewModel
import viewmodel.VideoFormat

@Composable
fun VideoConvertView() {
    val localizationState: LocalizationState = koinInject()
    val viewModel: VideoConvertViewModel = koinInject()
    val targetFormat = viewModel.targetFormat.collectAsState()
    val videoCodec = viewModel.videoCodec.collectAsState()
    var targetFormatExpanded by remember { mutableStateOf(false) }
    var videoCodecExpanded by remember { mutableStateOf(false) }
    var selectableCodecs by remember { mutableStateOf(VideoCodec.entries) }
    val isDragOver = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .commonDragAndDrop(
                isDragOver = isDragOver,
                onDrop = { viewModel.handleDrop(it, TargetPickerType.DIRECTORY) }
            )
            .background(if (isDragOver.value) Color.LightGray else Color.Transparent)
    ) {
        TitleTextSection(
            titleText = localizationState.getString("title_convert_video_format"),
            tooltip = localizationState.getString("tooltip_convert_video_format")
        )

        CheckboxSection(
            onClick = { viewModel.toggleUseHardwareEncoder() },
            checked = viewModel.useHardwareEncoder.collectAsState().value,
            text = localizationState.getString("use_hardware_encoding")
        )

        Row {
            DropdownMenuBox(
                label = "${localizationState.getString("target_format")}:",
                selectedItem = targetFormat.value,
                items = VideoFormat.entries,
                onItemSelected = {
                    viewModel.setTargetFormat(it)
                },
                expanded = targetFormatExpanded,
                onExpandedChange = { targetFormatExpanded = it }
            )

            Spacer(modifier = Modifier.width(16.dp))

            DropdownMenuBox(
                label = "${localizationState.getString("video_codec")}:",
                selectedItem = videoCodec.value,
                items = selectableCodecs,
                onItemSelected = { viewModel.setVideoCodec(it) },
                expanded = videoCodecExpanded,
                onExpandedChange = { videoCodecExpanded = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProcessesSection(viewModel)
    }
}