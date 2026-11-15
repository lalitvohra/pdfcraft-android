package io.github.artcraftandroid.pdfcraft.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.pdfcraft.R
import io.github.artcraftandroid.pdfcraft.text
import io.github.artcraftandroid.pdfcraft.ui.AppViewModel
import io.github.artcraftandroid.pdfcraft.ui.LucideIcons
import io.github.artcraftandroid.pdfcraft.ui.PdfSheet
import io.github.artcraftandroid.pdfcraft.ui.SheetRow
import io.github.artcraftandroid.pdfcraft.ui.pdfColors

/**
 * The sheet the tab strip's + button opens: the documents the reader has
 * opened before, so another one can be opened beside the one on screen.
 *
 * The open documents stay open behind it — this is how several are kept open
 * at once — and choosing one here brings it to the front beside the others.
 */
@Composable
fun OpenDocumentSheet(vm: AppViewModel, onBrowse: () -> Unit, onDismiss: () -> Unit) {
    val c = pdfColors
    val rows = remember { vm.recentRows() }
    val onDevice = remember { vm.localRows() }

    PdfSheet(onDismiss = onDismiss, maxHeightFraction = 0.85f) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LucideIcons.Files, null, tint = c.icon, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.viewer_open_another),
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        // Open any file on the phone, not just the ones opened before.
        SheetRow(
            LucideIcons.FolderOpen,
            stringResource(R.string.home_open_file),
            onClick = {
                onDismiss()
                onBrowse()
            },
        )
        androidx.compose.material3.HorizontalDivider(color = c.divider)

        if (rows.isEmpty() && onDevice.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(LucideIcons.Files, null, tint = c.textSecondary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    text(R.string.viewer_no_other_documents),
                    color = c.textSecondary,
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyColumn(Modifier.heightIn(max = 520.dp)) {
                if (rows.isNotEmpty()) {
                    item {
                        Text(
                            text(R.string.home_list_recent),
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(rows, key = { it.key }) { r ->
                        DocumentRow(r.name) {
                            onDismiss()
                            r.open()
                        }
                    }
                }
                if (onDevice.isNotEmpty()) {
                    item {
                        Text(
                            text(R.string.home_other_files),
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(onDevice, key = { it.key }) { r ->
                        DocumentRow(r.name) {
                            onDismiss()
                            r.open()
                        }
                    }
                }
            }
        }
    }
}

/** A document in the sheet: its name, and a tap opens it beside the current one. */
@Composable
private fun DocumentRow(name: String, onOpen: () -> Unit) {
    val c = pdfColors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LucideIcons.FileText, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            name.removeSuffix(".pdf"),
            color = c.text,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
