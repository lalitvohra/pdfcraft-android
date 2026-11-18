package io.github.artcraftandroid.pdfcraft.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.artcraftandroid.pdfcraft.ui.AppViewModel
import io.github.artcraftandroid.pdfcraft.ui.OpenDocument
import io.github.artcraftandroid.pdfcraft.ui.pdfColors

/**
 * The viewer's side panel: the Bookmarks & Table of Contents, kept open
 * beside the pages.
 *
 * The panel docks to the end edge. Its width is a fraction of the screen's
 * width that the reader can drag; the width and the open state are kept
 * between sessions, so the panel stays where the reader put it.
 *
 * The panel's edge carries a drag handle, and a tap on that edge closes the
 * panel.
 */
@Composable
fun SidePanelHost(
    vm: AppViewModel,
    doc: OpenDocument,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = pdfColors
    val density = LocalDensity.current
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    // The system button row sits over the bottom of the panel. The panel's
    // content stops above it, so the last bookmark can be seen and pressed;
    // padding it as **content padding** inside the list would still let a
    // row rest under the buttons when the list is scrolled to the end.
    //
    // It is `safeDrawing` and not `navigationBars`: the viewer hides the
    // system bars, so `navigationBars` reports nothing.
    val bottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }

    BoxWithConstraints(modifier.fillMaxHeight().background(c.surface)) {
        // The panel's own width, and the screen's, so a drag can be measured.
        val panelWidthPx = with(density) { maxWidth.toPx() }
        val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }.coerceAtLeast(1f)

        // While the panel is dragged its width follows the finger; the saved
        // width follows when the drag ends.
        var dragFraction by remember { mutableFloatStateOf(-1f) }
        val fraction = if (dragFraction >= 0f) dragFraction else vm.sidePanelWidthFraction

        Box(Modifier.fillMaxSize().padding(bottom = bottomInset)) {
            BookmarksPanelContent(
                vm = vm,
                doc = doc,
                onDismiss = onClose,
                modifier = Modifier.fillMaxSize(),
            )

            // The panel's edge: drag it to resize, tap it to close.
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(16.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures { onClose() }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragFraction >= 0f) vm.saveSidePanelWidthFraction(dragFraction)
                                dragFraction = -1f
                            },
                            onDragCancel = { dragFraction = -1f },
                        ) { change, dragAmount ->
                            change.consume()
                            val start = if (dragFraction >= 0f) dragFraction else vm.sidePanelWidthFraction
                            // The panel is on the end edge, so dragging left
                            // widens it. The drag is measured against the
                            // screen's width, so the fraction moves with it.
                            val deltaFraction = -dragAmount / screenWidthPx
                            dragFraction = (start + deltaFraction).coerceIn(0.2f, 0.6f)
                        }
                    },
            )
        }
    }
}
