package io.github.artcraftandroid.pdfcraft.ui

import io.github.artcraftandroid.pdfcraft.ui.viewer.pagesWidth
import io.github.artcraftandroid.pdfcraft.ui.viewer.scrubPage
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The page number navigator: the scrub that moves through the document, and
 * the width the pages keep when the side panel is docked beside them.
 */
class PageNumberNavigatorTest {
    @Test
    fun scrubbingCountsForwardFromThePageTheDragStartedOn() {
        // one page is 1000 px tall; a drag of 2500 px is two and a half pages
        assertEquals(2, scrubPage(base = 0, dragged = 2500f, pageHeight = 1000f, pages = 59))
        // the count runs from the page the drag started on
        assertEquals(7, scrubPage(base = 5, dragged = 2500f, pageHeight = 1000f, pages = 59))
        // a drag of less than a page stays on the page
        assertEquals(0, scrubPage(base = 0, dragged = 999f, pageHeight = 1000f, pages = 59))
        // a drag of nothing stays put
        assertEquals(7, scrubPage(base = 7, dragged = 0f, pageHeight = 1000f, pages = 59))
    }

    @Test
    fun scrubbingIsClampedToTheDocument() {
        assertEquals(0, scrubPage(base = 0, dragged = -5000f, pageHeight = 1000f, pages = 59))
        assertEquals(58, scrubPage(base = 58, dragged = 5000f, pageHeight = 1000f, pages = 59))
        // a document of one page has only page 0
        assertEquals(0, scrubPage(base = 0, dragged = 5000f, pageHeight = 1000f, pages = 1))
    }

    @Test
    fun aZeroHeightPageStillCounts() {
        // a page height of zero would divide by zero; the page must not move
        assertEquals(0, scrubPage(base = 0, dragged = 100f, pageHeight = 0f, pages = 59))
    }

    @Test
    fun thePagesKeepTheirWidthWhenTheSidePanelIsDocked() {
        // no panel: the pages have the whole row
        assertEquals(1000.dp, pagesWidth(1000.dp, 0.2f, panelOpen = false))
        // the panel takes its share of the row, and the pages get the rest
        assertEquals(800.dp, pagesWidth(1000.dp, 0.2f, panelOpen = true))
        assertEquals(400f, pagesWidth(1000.dp, 0.6f, panelOpen = true).value, 0.01f)
    }
}
