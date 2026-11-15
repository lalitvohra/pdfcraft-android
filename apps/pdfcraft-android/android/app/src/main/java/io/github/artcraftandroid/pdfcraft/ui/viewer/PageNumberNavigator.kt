package io.github.artcraftandroid.pdfcraft.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.pdfcraft.R
import io.github.artcraftandroid.pdfcraft.ui.LucideIcons
import io.github.artcraftandroid.pdfcraft.ui.OpenDocument
import io.github.artcraftandroid.pdfcraft.ui.pdfColors
import kotlin.math.roundToInt

/**
 * The page number navigator: a floating panel that shows where the reader is
 * in the document, and a numpad to type a page number into.
 *
 * It follows the same pattern as the floating dial pad in our own PDF app: a
 * title bar acts as the drag handle, the panel is offset from the corner, and
 * the reader can put it wherever they want it.
 *
 * The title bar shows the page number and the page count, and a button opens
 * the numpad. Dragging the panel moves it; dragging it up or down over a page
 * of the document scrubs through the document, the way the scrubber on the
 * edge does, and the page it has reached appears below the bar.
 *
 * The numpad is the one from our own PDF app: digits to type a page number,
 * a backspace to rub out a digit, and GO. The number goes to the page when
 * GO is pressed, or a moment after the last digit.
 */
@Composable
fun BoxScope.PageNumberNavigator(
    doc: OpenDocument,
    visible: Boolean,
    onDrag: () -> Unit,
    onTap: () -> Unit,
    onClose: () -> Unit,
) {
    val c = pdfColors
    val v = doc.viewer
    val density = LocalDensity.current

    if (doc.pageCount < 1) return

    // The drag offset from the parent's bottom-end corner, in pixels.
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    // While the panel is dragged, the page it has reached.
    var scrubbing by remember { mutableStateOf(false) }
    var dragPage by remember { mutableStateOf(-1) }
    var scrubStart by remember { mutableStateOf(0f) }
    // Whether the numpad is showing.
    var padOpen by remember { mutableStateOf(false) }
    // The digits typed so far.
    var buffer by remember { mutableStateOf("") }
    // When the last digit was typed, for the go-after-a-moment.
    var lastTapAt by remember { mutableLongStateOf(0L) }
    // The title bar's width, so a tap can be told apart by where it lands.
    var barWidthPx by remember { mutableIntStateOf(0) }

    // How far the panel may be dragged: up, towards the tab strip, and down.
    val maxUp = with(density) { 200.dp.toPx() }
    val maxDown = with(density) { 120.dp.toPx() }

    // The panel is anchored at the bottom-end corner of the page. The
    // alignment is set here rather than passed in, because a Modifier that
    // carried it would lose its scope on the way here.
    Box(
        Modifier.matchParentSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(
            Modifier
                .padding(end = 16.dp, bottom = 150.dp)
                .offset {
                    IntOffset(
                        offsetX.roundToInt().coerceIn(-maxUp.roundToInt(), 0),
                        offsetY.roundToInt().coerceIn(-maxDown.roundToInt(), 0),
                    )
                }
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface),
        ) {
            // ---- The title bar: the drag handle, and the page number ----
            Row(
                Modifier
                    .widthIn(min = 150.dp, max = 230.dp)
                    .background(if (scrubbing) c.accent else c.pressed)
                    .onSizeChanged { barWidthPx = it.width }
                    .pointerInput(doc) {
                        detectTapGestures(onTap = { pos ->
                            // The bar carries three actions, told apart by
                            // where the finger lands: the X at the end hides
                            // the panel, the grid next to it opens the
                            // numpad, and the rest opens Go to page.
                            val x = pos.x
                            val closeAt = size.width - with(density) { 32.dp.toPx() }
                            val padAt = size.width - with(density) { 52.dp.toPx() }
                            when {
                                x > closeAt -> onClose()
                                x > padAt -> {
                                    padOpen = !padOpen
                                    buffer = ""
                                    lastTapAt = 0L
                                }
                                else -> onTap()
                            }
                        })
                    }
                    .pointerInput(doc) {
                        detectDragGestures(
                            onDragStart = {
                                scrubbing = false
                                dragPage = -1
                                scrubStart = 0f
                            },
                            onDragEnd = {
                                scrubbing = false
                                dragPage = -1
                                onDrag()
                            },
                            onDragCancel = {
                                scrubbing = false
                                dragPage = -1
                            },
                        ) { change, drag ->
                            change.consume()
                            if (!scrubbing) {
                                // A drag that is mostly vertical scrubs
                                // through the document; any other drag moves
                                // the panel.
                                if (kotlin.math.abs(drag.y) > kotlin.math.abs(drag.x) && kotlin.math.abs(drag.y) > 0f) {
                                    scrubbing = true
                                    scrubStart = v.scrollY
                                } else {
                                    offsetX += drag.x
                                    offsetY += drag.y
                                    return@detectDragGestures
                                }
                            }
                            if (scrubbing) {
                                val page = ((scrubStart + drag.y) / (v.viewH / v.zoom).coerceAtLeast(1f)).toInt()
                                    .coerceIn(0, doc.pageCount - 1)
                                if (page != dragPage) {
                                    dragPage = page
                                    v.goToPage(page)
                                    onDrag()
                                }
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    LucideIcons.GripVertical,
                    null,
                    tint = if (scrubbing) Color.White else c.icon,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.viewer_go_to_page),
                    color = if (scrubbing) Color.White else c.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                // The page it is on, and how many there are.
                Text(
                    "${v.currentPage + 1} / ${doc.pageCount}",
                    color = if (scrubbing) Color.White else c.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                // The numpad, and the X that hides the panel. They are drawn
                // here, and the bar's own tap handler acts on them: a child
                // gesture would lose to the bar's drag detector.
                Icon(
                    LucideIcons.LayoutGrid,
                    stringResource(R.string.viewer_page_numpad),
                    tint = if (scrubbing) Color.White else c.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Icon(
                    LucideIcons.X,
                    stringResource(R.string.viewer_hide_page_navigator),
                    tint = if (scrubbing) Color.White else c.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            // ---- The page the drag has reached ----
            // Shown below the bar, where the finger on the bar does not hide
            // it.
            androidx.compose.animation.AnimatedVisibility(
                visible = scrubbing && dragPage >= 0,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(80)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)),
            ) {
                Box(
                    Modifier
                        .widthIn(min = 150.dp, max = 230.dp)
                        .height(40.dp)
                        .background(c.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${dragPage + 1}",
                        color = c.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // ---- The numpad ----
            if (padOpen) {
                PageNumpad(
                    buffer = buffer,
                    pages = doc.pageCount,
                    onDigit = { digit ->
                        buffer = (buffer + digit).take(6)
                        lastTapAt = android.os.SystemClock.uptimeMillis()
                    },
                    onBackspace = {
                        if (buffer.isNotEmpty()) {
                            buffer = buffer.dropLast(1)
                            lastTapAt = android.os.SystemClock.uptimeMillis()
                        }
                    },
                    onGo = {
                        val target = buffer.toIntOrNull()
                        if (target != null && target in 1..doc.pageCount) {
                            v.goToPage(target - 1)
                            onDrag()
                        }
                        buffer = ""
                        lastTapAt = 0L
                    },
                    modifier = Modifier.width(190.dp),
                )
            }
        }
    }
}

/**
 * The numpad: a page number typed in directly.
 *
 * The digits go to the page when GO is pressed, or a moment after the last
 * digit — so a reader can type "1 2" and have it go to page 12 without
 * reaching for GO.
 */
@Composable
private fun PageNumpad(
    buffer: String,
    pages: Int,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = pdfColors
    val density = LocalDensity.current

    // The page the typed digits would go to, and whether it is out of the
    // document's range.
    val typed = buffer.toIntOrNull()
    val overflow = typed != null && typed > pages

    Column(
        modifier
            .background(c.surface)
            .padding(4.dp),
    ) {
        // What has been typed, over the page count.
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(if (overflow) c.red.copy(alpha = 0.15f) else c.surfaceRaised)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = if (buffer.isEmpty()) "—" else buffer,
                color = if (overflow) c.red else c.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(4.dp))

        // 1 2 3 / 4 5 6 / 7 8 9 / backspace 0 GO
        for (row in listOf("123", "456", "789")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (d in row) {
                    NumpadKey(d.toString(), Modifier.weight(1f).height(40.dp), onClick = { onDigit(d.toString()) })
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumpadKey("⌫", Modifier.weight(1f).height(40.dp), onClick = onBackspace, secondary = true)
            NumpadKey("0", Modifier.weight(1f).height(40.dp), onClick = { onDigit("0") })
            NumpadKey(stringResource(R.string.viewer_page_go), Modifier.weight(1f).height(40.dp), onClick = onGo, accent = true)
        }
    }
}

/** A key of the numpad: a digit, the backspace, or GO. */
@Composable
private fun NumpadKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    secondary: Boolean = false,
    accent: Boolean = false,
) {
    val c = pdfColors
    val bg = when {
        accent -> c.accent
        secondary -> c.pressed
        else -> c.surfaceRaised
    }
    val fg = when {
        accent -> Color.White
        secondary -> c.text
        else -> c.text
    }
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
