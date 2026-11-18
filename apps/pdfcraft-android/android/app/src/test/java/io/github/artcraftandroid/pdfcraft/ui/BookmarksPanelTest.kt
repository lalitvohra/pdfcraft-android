package io.github.artcraftandroid.pdfcraft.ui

import io.github.artcraftandroid.pdfcraft.ui.viewer.initialPanelTab
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bookmarks panel: which tab a document opens on.
 *
 * The panel is reused when the reader switches documents — the same
 * composable with a new document — so its state is remembered per document.
 * Without that, switching to a document with no outline still showed the
 * previous document's contents.
 */
class BookmarksPanelTest {
    @Test
    fun aDocumentWithAnOutlineOpensOnItsContents() {
        assertEquals(0, initialPanelTab(hasOutline = true))
    }

    @Test
    fun aDocumentWithoutAnOutlineOpensOnItsBookmarks() {
        assertEquals(1, initialPanelTab(hasOutline = false))
    }
}
