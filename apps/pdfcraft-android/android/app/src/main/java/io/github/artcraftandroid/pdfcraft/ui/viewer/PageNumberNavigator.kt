package io.github.artcraftandroid.pdfcraft.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
 * The page number navigator: a small panel that floats over the viewer and
 * shows where the reader is in the document.
 *
 * It follows the same pattern as the floating annotation bar in our own PDF
 * app: a title bar acts as the drag handle, and the panel is offset from the
 * parent's top-left, so the reader can put it wherever they want it and it
 * stays there.
 *
 * The title bar shows the page number and the page count. Dragging the body of
 * the panel moves it; dragging the title bar moves it and, if the reader drags
 * it far enough, scrubs through the document, the way the scrubber on the edge
 * does. A tap on the title opens Go to page.
 *
 * The X on the title bar hides the panel; the reader brings it back from the
 * viewer's menu.
 */
@Composable
fun PageNumberNavigator(
    doc: OpenDocument,
    visible: Boolean,
    onDrag: () -> Unit,
    onTap: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = pdfColors
    val v = doc.viewer
    val density = LocalDensity.current

    if (doc.pageCount < 1) return

    // The drag offset from the parent's top-left, in pixels.
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // While the title bar is dragged, the page it has reached, and whether
    // the drag has moved far enough to scrub.
    var scrubbing by remember { mutableStateOf(false) }
    var dragPage by remember { mutableIntStateOf(-1) }
    var scrubStart by remember { mutableStateOf(0f) }

    // The height of a page of the document, so a drag of one screen's height
    // moves one page.
    val pageHeightPx = with(density) { (v.viewH / v.zoom).coerceAtLeast(1f) }

    val described = stringResource(R.string.viewer_page_of, v.currentPage + 1, doc.pageCount)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(200)),
    ) {
        Column(
            modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface),
        ) {
            // ---- The title bar: the drag handle, and the page number ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (scrubbing) c.accent else c.pressed)
                    .pointerInput(doc) {
                        detectTapGestures(onTap = { onTap() })
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
                            // A drag along the panel moves it; a drag across it
                            // (or a long one) scrubs through the document.
                            if (!scrubbing && dragPage < 0) {
                                val along = abs(drag.y) > abs(drag.x)
                                if (along && abs(drag.y) > 0f) {
                                    // the drag is vertical: it scrubs
                                    scrubbing = true
                                    scrubStart = v.scrollY
                                    offsetX += drag.x
                                    offsetY += drag.y
                                    return@detectDragGestures
                                }
                                // otherwise the panel follows the finger
                                offsetX += drag.x
                                offsetY += drag.y
                                return@detectDragGestures
                            }
                            if (scrubbing) {
                                val page = ((scrubStart + drag.y) / pageHeightPx).toInt()
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
                Spacer(Modifier.width(6.dp))
                Text(
                    "${v.currentPage + 1} / ${doc.pageCount}",
                    color = if (scrubbing) Color.White else c.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    LucideIcons.X,
                    stringResource(R.string.viewer_hide_page_navigator),
                    tint = if (scrubbing) Color.White else c.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { onClose() }
                        },
                )
            }

            // ---- The body: the page the drag has reached ----
            // A small strip that shows the page under the finger, so the
            // reader can see it without the finger hiding it.
            AnimatedVisibility(
                visible = scrubbing && dragPage >= 0,
                enter = fadeIn(tween(80)),
                exit = fadeOut(tween(150)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(c.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${dragPage + 1}",
                        color = c.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun abs(f: Float): Float = if (f < 0) -f else f
