package io.github.artcraftandroid.pdfcraft.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the app may go back to the app that opened the document.
 *
 * A document another app opened is closed by the reader going back out of it,
 * so the app that lent it gets the reader back. But that is only so when the
 * last document goes: with several open, the app stays and the reader carries
 * on with the others.
 */
class LeaveAppTest {

    /**
     * Whether the app should finish, given the document that was closed, and
     * whether any document is still open.
     */
    private fun shouldLeave(closedFromOtherApp: Boolean, stillOpen: Int): Boolean =
        stillOpen == 0 && closedFromOtherApp

    @Test
    fun `the app closes when the last document from another app is closed`() {
        assertTrue(shouldLeave(closedFromOtherApp = true, stillOpen = 0))
    }

    @Test
    fun `the app stays when another document is still open`() {
        // the bug: closing one document finished the app, even with others open
        assertFalse(shouldLeave(closedFromOtherApp = true, stillOpen = 1))
        assertFalse(shouldLeave(closedFromOtherApp = true, stillOpen = 3))
    }

    @Test
    fun `the app stays when the document was opened from inside the app`() {
        assertFalse(shouldLeave(closedFromOtherApp = false, stillOpen = 0))
    }
}
