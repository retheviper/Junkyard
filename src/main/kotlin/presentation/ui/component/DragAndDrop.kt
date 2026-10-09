package presentation.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.commonDragAndDrop(
    isDragOver: MutableState<Boolean>,
    onDrop: (DragAndDropEvent) -> Boolean
): Modifier {
    // The remembered target outlives recompositions, so it must call the latest onDrop rather than the first one.
    val currentOnDrop by rememberUpdatedState(onDrop)
    val target = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isDragOver.value = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragOver.value = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragOver.value = false
                return currentOnDrop(event)
            }
        }
    }

    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { true },
        target = target
    )
}
