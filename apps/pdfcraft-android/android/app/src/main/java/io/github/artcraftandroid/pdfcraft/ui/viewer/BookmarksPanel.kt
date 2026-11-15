package io.github.artcraftandroid.pdfcraft.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.pdfcraft.R
import io.github.artcraftandroid.pdfcraft.core.OutlineEntry
import io.github.artcraftandroid.pdfcraft.text
import io.github.artcraftandroid.pdfcraft.ui.AppViewModel
import io.github.artcraftandroid.pdfcraft.ui.LucideIcons
import io.github.artcraftandroid.pdfcraft.ui.OpenDocument
import io.github.artcraftandroid.pdfcraft.ui.pdfColors

/**
 * The Bookmarks & Table of Contents panel: the document's outline and the
 * reader's own bookmarks, with a tab for each.
 *
 * This is the same content the Bookmarks sheet shows, extracted so that it
 * can live in the side panel as well. The panel keeps it open beside the
 * pages, so the reader can jump around a long document without dismissing
 * anything first.
 *
 * @param onDismiss called when the panel is closed (its close button).
 */
@Composable
fun BookmarksPanelContent(
    vm: AppViewModel,
    doc: OpenDocument,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = pdfColors
    var outline by remember { mutableStateOf<List<OutlineEntry>?>(null) }
    LaunchedEffect(doc.info.generation) { outline = vm.outline(doc) }
    val hasOutline = doc.info.hasOutline
    var tab by remember { mutableStateOf(if (hasOutline) 0 else 1) }
    // the outline level shown: the index of its parent entry (-1: the top level)
    var parent by remember { mutableStateOf(-1) }
    var renaming by remember { mutableStateOf<Int?>(null) }

    Column(modifier.fillMaxSize().background(c.surface)) {
        // The panel's own header: its title, and a way to close it.
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text(R.string.sheet_bookmarks_contents),
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(LucideIcons.X, text(R.string.viewer_close), tint = c.icon)
            }
        }

        // The two tabs: the document's contents, and the reader's bookmarks.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf(text(R.string.sheet_tab_contents), text(R.string.sheet_tab_bookmarks)).forEachIndexed { i, label ->
                Column(
                    Modifier.weight(1f).clickable(role = Role.Tab) { tab = i }.padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        color = if (tab == i) c.text else c.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth().height(2.dp)
                            .background(if (tab == i) c.accent else Color.Transparent),
                    )
                }
            }
        }
        SheetDivider(inset = 0.dp)

        if (tab == 0) {
            val all = outline
            when {
                all == null -> Text("…", color = c.textSecondary, modifier = Modifier.padding(20.dp))
                all.isEmpty() -> Text(
                    text(R.string.sheet_no_contents),
                    color = c.textSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(20.dp),
                )
                else -> {
                    val level = if (parent < 0) 0u else all[parent].level + 1u
                    val start = parent + 1
                    // the entries of this level under `parent`: until the level rises back above it
                    val entries = buildList {
                        var i = start
                        while (i < all.size) {
                            val e = all[i]
                            if (e.level < level) break
                            if (e.level == level) add(i)
                            i++
                        }
                    }
                    LazyColumn(Modifier.weight(1f)) {
                        if (parent >= 0) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().clickable { parent = parentOf(all, parent) }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        LucideIcons.ArrowLeft,
                                        text(R.string.sheet_up_level),
                                        tint = c.icon,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        all[parent].title,
                                        color = c.text,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        items(entries) { i ->
                            val e = all[i]
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    e.page?.let { p -> doc.viewer.goToPage(p.toInt()) }
                                }.padding(start = 20.dp, end = 4.dp).heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    e.title,
                                    color = c.text,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (e.children > 0u) {
                                    BarIcon(
                                        LucideIcons.ChevronRight,
                                        text(R.string.sheet_expand_entry, e.title),
                                        { parent = i },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val list = vm.bookmarks.list(doc)
            if (list.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(LucideIcons.Bookmark, null, tint = c.textSecondary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(text(R.string.sheet_bookmarks_empty), color = c.text, fontSize = 15.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text(R.string.sheet_add_a_bookmark),
                        color = c.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { vm.toggleBookmark(doc) }.padding(8.dp),
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(list.size) { i ->
                        val b = list[i]
                        UserBookmarkRow(
                            name = b.name,
                            page = b.page,
                            renaming = renaming == i,
                            onOpen = { doc.viewer.goToPage(b.page) },
                            onRename = { renaming = i },
                            onRenamed = { name ->
                                renaming = null
                                if (name.isBlank()) vm.tell(text(R.string.sheet_bookmark_name_invalid))
                                else vm.bookmarks.rename(doc, b, name.trim())
                            },
                            onDelete = {
                                vm.bookmarks.remove(doc, b)
                                vm.tell(text(R.string.sheet_bookmark_removed, b.name))
                            },
                            onDeleteAll = { vm.confirmDeleteAllBookmarks = doc },
                        )
                    }
                }
            }
        }
    }
}

/** The index of the parent of the outline entry at `i`, or -1 for a top-level one. */
internal fun parentOf(all: List<OutlineEntry>, i: Int): Int {
    val level = all[i].level
    if (level == 0u) return -1
    var k = i - 1
    while (k >= 0 && all[k].level >= level) k--
    return k
}
