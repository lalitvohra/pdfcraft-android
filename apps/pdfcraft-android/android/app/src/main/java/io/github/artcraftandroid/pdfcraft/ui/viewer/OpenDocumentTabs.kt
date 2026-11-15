package io.github.artcraftandroid.pdfcraft.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.pdfcraft.R
import io.github.artcraftandroid.pdfcraft.text
import io.github.artcraftandroid.pdfcraft.ui.AppViewModel
import io.github.artcraftandroid.pdfcraft.ui.LucideIcons
import io.github.artcraftandroid.pdfcraft.ui.OpenDocument
import io.github.artcraftandroid.pdfcraft.ui.pdfColors

/**
 * The tab strip: the documents the reader has open, so several PDFs can be
 * open at once and the reader can move between them.
 *
 * - A tap on a tab brings that document to the front.
 * - The X on a tab closes that document.
 * - A long press on a tab and a drag reorders the tabs.
 * - The + button opens another document.
 */
@Composable
fun OpenDocumentTabs(
    vm: AppViewModel,
    documents: List<OpenDocument>,
    current: OpenDocument?,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = pdfColors
    if (documents.isEmpty()) return

    val listState = rememberLazyListState()
    val selectedIndex = documents.indexOfFirst { it === current }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }

    Row(
        modifier.fillMaxWidth().background(c.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The key must be a type Android can keep in a Bundle, so the
            // document's engine id (a ULong) cannot be one: the index is.
            itemsIndexed(documents, key = { index, _ -> index }) { index, doc ->
                ReorderableTabChip(
                    title = doc.title,
                    selected = doc === current,
                    onClick = { onSelect(index) },
                    onClose = { onClose(index) },
                    onDrag = onReorder,
                    indexInList = index,
                )
            }
            item(key = -1) {
                IconButton(onClick = onNewTab, modifier = Modifier.size(38.dp)) {
                    Icon(
                        LucideIcons.Plus,
                        text(R.string.viewer_new_tab),
                        tint = c.icon,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * A tab that can be dragged to a new place.
 *
 * The drag starts on a long press, so a plain tap still selects the tab. The
 * dragged chip moves with the finger; once it has travelled half its own
 * width past a neighbour, the two swap and the finger keeps its place.
 */
@Composable
private fun ReorderableTabChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Int, Int) -> Unit,
    indexInList: Int,
) {
    val c = pdfColors
    val density = LocalDensity.current

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }
    var accumulatedDelta by remember { mutableFloatStateOf(0f) }
    var itemWidthPx by remember { mutableIntStateOf(0) }

    val isDragging = draggingIndex == indexInList

    Box(
        Modifier
            .onSizeChanged { itemWidthPx = it.width }
            .pointerInput(indexInList) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        draggingIndex = indexInList
                        draggingOffset = 0f
                        accumulatedDelta = 0f
                    },
                    onDragEnd = {
                        draggingIndex = null
                        draggingOffset = 0f
                        accumulatedDelta = 0f
                    },
                    onDragCancel = {
                        draggingIndex = null
                        draggingOffset = 0f
                        accumulatedDelta = 0f
                    },
                ) { _, dragAmount ->
                    accumulatedDelta += dragAmount.x
                    draggingOffset = accumulatedDelta
                    if (itemWidthPx > 0) {
                        val halfWidth = itemWidthPx / 2f
                        val from = indexInList
                        val target = when {
                            accumulatedDelta > halfWidth -> indexInList + 1
                            accumulatedDelta < -halfWidth -> indexInList - 1
                            else -> indexInList
                        }
                        if (target != from) {
                            onDrag(from, target)
                            // The chip now sits in a new slot: take the width
                            // that got it there off the accumulated delta, so
                            // the reader keeps dragging from their finger and
                            // not from the snap point.
                            accumulatedDelta -= (target - from) * itemWidthPx
                            draggingOffset = accumulatedDelta
                        }
                    }
                }
            }
            .graphicsLayer {
                translationX = if (isDragging) draggingOffset else 0f
                shadowElevation = if (isDragging) with(density) { 8.dp.toPx() } else 0f
                alpha = if (isDragging) 0.92f else 1f
            },
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) c.pressed else Color.Transparent)
                .clickable { onClick() }
                .heightIn(min = 34.dp)
                .padding(start = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                color = if (selected) c.text else c.textSecondary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(132.dp),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(
                    LucideIcons.X,
                    text(R.string.viewer_close),
                    tint = c.textSecondary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
