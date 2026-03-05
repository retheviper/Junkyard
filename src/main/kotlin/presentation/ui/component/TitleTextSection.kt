package presentation.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TitleTextSection(titleText: String, tooltip: String) {
    Row(
        modifier = Modifier.padding(8.dp),
    ) {
        Text(
            titleText,
            fontSize = 26.sp
        )

        TooltipArea(
            tooltip = {
                Surface(
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colors.secondary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tooltip,
                        color = MaterialTheme.colors.onSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))
}
