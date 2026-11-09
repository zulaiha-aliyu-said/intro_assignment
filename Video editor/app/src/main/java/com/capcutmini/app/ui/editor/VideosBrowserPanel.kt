package com.capcutmini.app.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.capcutmini.app.media.library.DeviceVideo
import com.capcutmini.app.ui.components.MediaThumbnail
import com.capcutmini.app.ui.components.formatDuration

@Composable
fun VideosBrowserPanel(
    hasPermission: Boolean,
    loading: Boolean,
    error: String?,
    videos: List<DeviceVideo>,
    staged: List<StagedClip>,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onBrowseFolders: () -> Unit,
    onMultiPick: () -> Unit,
    onStageLibraryVideo: (DeviceVideo) -> Unit,
    onRemoveStaged: (String) -> Unit,
    onClearStaged: () -> Unit,
    onCommitStaged: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Videos", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Browse device videos or pick multiple files. Staged clips can be removed before adding to the timeline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onMultiPick) { Text("Select multiple") }
            OutlinedButton(onClick = onBrowseFolders) { Text("Browse folders") }
        }

        if (!hasPermission) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "To list all videos in a scrollable grid, allow media access. You can still pick files without this permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRequestPermission) { Text("Allow video access") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            }
            when {
                loading -> Text("Loading videos…")
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                videos.isEmpty() -> Text("No videos found. Try Browse folders.")
                else -> {
                    Text("${videos.size} videos", style = MaterialTheme.typography.bodyMedium)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(videos, key = { it.id }) { video ->
                            Column(
                                modifier = Modifier
                                    .clickable { onStageLibraryVideo(video) }
                                    .padding(4.dp)
                            ) {
                                MediaThumbnail(uri = video.uri, size = 96.dp)
                                Text(
                                    text = video.displayName.take(18),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatDuration(video.durationMs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (staged.isNotEmpty()) {
            Text("Staged (${staged.size})", style = MaterialTheme.typography.titleLarge)
            staged.forEach { clip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MediaThumbnail(uri = clip.uri, size = 56.dp)
                        Column {
                            Text(clip.displayName.take(28))
                            Text(
                                formatDuration(clip.durationMs),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = { onRemoveStaged(clip.id) }) { Text("Remove") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCommitStaged) { Text("Add to timeline") }
                TextButton(onClick = onClearStaged) { Text("Clear") }
            }
        }
    }
}
