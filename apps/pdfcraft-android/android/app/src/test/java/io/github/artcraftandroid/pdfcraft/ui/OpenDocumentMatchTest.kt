package io.github.artcraftandroid.pdfcraft.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The rule that decides whether a document the reader picked is already open.
 *
 * The reader can keep several documents open at once and move between them
 * from the tab strip. Opening a document that is already open should bring
 * that one to the front; opening a different one should add it to the open
 * ones.
 *
 * The working copy's path is what tells two documents apart. The URI cannot:
 * a document opened from another app's own copy has no source URI at all, so
 * comparing the sources would make every such document look like the same
 * one, and the second document would never open.
 */
class OpenDocumentMatchTest {

    /** The working copies of the open documents, and their source URIs. */
    private data class Open(val file: File, val source: String?)

    /** What [AppViewModel.openFile] uses to find a document that is already open. */
    private fun findOpen(open: List<Open>, file: File): Open? =
        open.firstOrNull { it.file.absolutePath == file.absolutePath }

    private fun doc(path: String, source: String? = null) = Open(File(path), source)

    @Test
    fun `two documents with no source are different documents`() {
        val open = listOf(doc("/data/work/first.pdf"), doc("/data/work/second.pdf"))
        val second = File("/data/work/second.pdf")

        // The old rule compared the sources, and both are null, so the first
        // document matched and the second one never opened.
        val oldMatch = open.firstOrNull { it.source == null }
        assertEquals("the old rule wrongly matched", "/data/work/first.pdf", oldMatch?.file?.absolutePath)

        // The new rule tells them apart by the working copy: it finds the
        // document that was really asked for, not the first one.
        assertEquals("/data/work/second.pdf", findOpen(open, second)?.file?.absolutePath)
    }

    @Test
    fun `a document already open is found by its working copy`() {
        val open = listOf(doc("/data/work/first.pdf"), doc("/data/work/second.pdf"))
        val second = File("/data/work/second.pdf")

        assertNotNull(findOpen(open, second))
        assertEquals("/data/work/second.pdf", findOpen(open, second)?.file?.absolutePath)
    }

    @Test
    fun `the same document opened again is found`() {
        val open = listOf(doc("/data/work/first.pdf", "content://a/1"))
        val same = File("/data/work/first.pdf")

        assertNotNull(findOpen(open, same))
    }

    @Test
    fun `a document with the same name in another folder is a different document`() {
        val open = listOf(doc("/data/work/report.pdf", "content://a/1"))
        val other = File("/data/other/report.pdf")

        assertNull(findOpen(open, other))
    }
}
