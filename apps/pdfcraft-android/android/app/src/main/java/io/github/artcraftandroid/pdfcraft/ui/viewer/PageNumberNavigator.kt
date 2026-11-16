package io.github.artcraftandroid.pdfcraft.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.unit.Dp
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
    // The panel is shown only when the reader asks for it: the bar's X and
    // the top-bar button both clear this flag. Without this the panel would
    // stay on screen whatever the flag said, and the X would do nothing.
    if (!visible) return

    // The drag offset from the parent's bottom-end corner, in pixels. It is
    // not clamped: the panel may be put anywhere on the page, the way the
    // dial pad in our own app may be.
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    // While the panel is dragged, the page it has reached.
    var scrubbing by remember { mutableStateOf(false) }
    var dragPage by remember { mutableStateOf(-1) }
    var scrubStart by remember { mutableStateOf(0f) }
    // The page the scrub started on: the count runs from here, so that going
    // to a page mid-drag does not shift the base.
    var scrubBase by remember { mutableIntStateOf(0) }
    // Whether the numpad is showing.
    var padOpen by remember { mutableStateOf(false) }
    // The digits typed so far.
    var buffer by remember { mutableStateOf("") }
    // When the last digit was typed, for the go-after-a-moment.
    var lastTapAt by remember { mutableLongStateOf(0L) }
    // The title bar's width, so a tap can be told apart by where it lands.
    var barWidthPx by remember { mutableIntStateOf(0) }

    // The panel is anchored at the bottom-end corner of the pages, not of
    // the whole viewer: the side panel is docked at the end edge beside the
    // pages, and the panel must not start underneath it. The alignment is
    // set here rather than passed in, because a Modifier that carried it
    // would lose its scope on the way here.
    Box(
        Modifier.matchParentSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(
            Modifier
                .padding(end = 16.dp, bottom = 150.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
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
                            onDragStart = { onDrag() },
                        ) { change, drag ->
                            change.consume()
                            // The bar is the drag handle: it moves the panel,
                            // in every direction. Scrubbing through the
                            // document is on the grip icon, so the two do not
                            // fight over the same gesture.
                            offsetX += drag.x
                            offsetY += drag.y
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The grip is not here: it lives in its own row below the
                // bar, so the bar's drag detector does not swallow the
                // scrubbing gesture. The reference's dial pad does the same
                // with its resize grip.
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

            // ---- The scrub strip ----
            // Its own row, with its own drag detector: the bar's detector
            // would swallow the gesture if the grip were inside the bar.
            // Dragging it up moves forward through the document.
            Box(
                Modifier
                    .widthIn(min = 150.dp, max = 230.dp)
                    .height(40.dp)
                    .background(c.surfaceRaised)
                    .pointerInput(doc) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                scrubbing = true
                                dragPage = v.currentPage
                                scrubStart = 0f
                                // The page the drag started on: the count
                                // runs from here, so that going to a page
                                // mid-drag does not shift the base.
                                scrubBase = v.currentPage
                            },
                            onDragEnd = { scrubbing = false; dragPage = -1 },
                            onDragCancel = { scrubbing = false; dragPage = -1 },
                        ) { change, dragAmount ->
                            change.consume()
                            // Dragging up goes forward, the way scrolling up
                            // the document does.
                            scrubStart -= dragAmount
                            val page = scrubPage(scrubBase, scrubStart, v.viewH / v.zoom, doc.pageCount)
                            if (page != dragPage) {
                                dragPage = page
                                v.goToPage(page)
                                onDrag()
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        LucideIcons.GripVertical,
                        stringResource(R.string.viewer_page_navigator_scrub),
                        tint = if (scrubbing) c.accent else c.icon,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (dragPage >= 0) "${dragPage + 1}" else "${v.currentPage + 1}",
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

/**
 * The page a scrub has reached: [base] plus the pages the drag has covered.
 *
 * [dragged] is how far the finger has travelled up the page (positive is
 * forward), [pageHeight] the height of one page in pixels, and [pages] the
 * number of pages in the document. The result is clamped to the document.
 *
 * The count runs from the page the drag started on, not from the scroll
 * position: the scroll only moves once the page has changed, so deriving
 * the page from it would never leave the first page.
 */
internal fun scrubPage(base: Int, dragged: Float, pageHeight: Float, pages: Int): Int {
    // A page height of zero would divide by zero, and the drag would then
    // throw the reader to the last page: a page that cannot be measured
    // simply does not move the count.
    if (pageHeight <= 0f) return base.coerceIn(0, (pages - 1).coerceAtLeast(0))
    return (base + (dragged / pageHeight).toInt()).coerceIn(0, (pages - 1).coerceAtLeast(0))
}

/**
 * The width of the pages, given the width of the row and the fraction the
 * side panel takes of it. The side panel is docked at the end edge beside
 * the pages, so the pages get the rest.
 */
internal fun pagesWidth(rowWidth: Dp, panelFraction: Float, panelOpen: Boolean): Dp =
    if (panelOpen) rowWidth * (1f - panelFraction) else rowWidth
