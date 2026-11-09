package com.capcutmini.app.ui.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capcutmini.app.R
import com.capcutmini.app.media.MediaMetadataReader
import com.capcutmini.app.ui.components.MediaThumbnail
import com.capcutmini.app.ui.components.VideoPlayerView
import com.capcutmini.app.ui.components.formatDuration
import kotlin.math.roundToLong

@Composable
fun EditorScreen(
    viewModel: EditorViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val frame by viewModel.previewController.frame.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.previewController.player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.previewController.player.pause()
        }
    }

    fun persistUris(uris: List<Uri>) {
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    }

    val multiVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        persistUris(uris)
        viewModel.stageUris(uris)
        viewModel.selectTab(EditorTab.VIDEOS)
    }

    val folderVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        persistUris(uris)
        viewModel.stageUris(uris)
        viewModel.selectTab(EditorTab.VIDEOS)
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val name = MediaMetadataReader.displayName(context, uri)
        viewModel.setAudioTrack(uri, name)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onVideoPermissionResult(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Video Editor",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        VideoPlayerView(
            player = viewModel.previewController.player,
            hasVideo = state.document.clips.isNotEmpty(),
            emptyMessage = stringResource(R.string.no_video_selected),
            blackFadeAlpha = frame.videoBlackAlpha,
            subtitleText = frame.activeSubtitle,
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Button(onClick = {
                multiVideoPicker.launch(arrayOf("video/*"))
            }) {
                Text("Select videos")
            }
            OutlinedButton(onClick = { viewModel.selectTab(EditorTab.VIDEOS) }) {
                Text("Videos")
            }
            Button(onClick = {
                audioPicker.launch(arrayOf("audio/*", "audio/mpeg", "audio/mp4", "audio/wav", "audio/x-wav"))
            }) {
                Text("Add Audio")
            }
        }

        if (state.document.clips.isNotEmpty()) {
            Text("Timeline", style = MaterialTheme.typography.titleLarge)
            state.document.clips.forEachIndexed { index, clip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaThumbnail(uri = Uri.parse(clip.uri), size = 56.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        FilterChip(
                            selected = state.selectedClip?.id == clip.id,
                            onClick = { viewModel.selectClip(clip.id) },
                            label = {
                                Text("${index + 1}. ${clip.displayName.take(16)} (${formatDuration(clip.trimmedDurationMs)})")
                            }
                        )
                        Row {
                            TextButton(
                                onClick = { viewModel.moveClip(clip.id, -1) },
                                enabled = index > 0
                            ) { Text("Up") }
                            TextButton(
                                onClick = { viewModel.moveClip(clip.id, 1) },
                                enabled = index < state.document.clips.lastIndex
                            ) { Text("Down") }
                            TextButton(onClick = { viewModel.removeClip(clip.id) }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }

        state.document.audioTrack?.let { audio ->
            Text(
                text = "Audio: ${audio.displayName} @ ${formatDuration(audio.startOffsetMs)} vol ${(audio.volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            EditorTab.entries.forEach { tab ->
                FilterChip(
                    selected = state.tab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    label = { Text(tab.name.lowercase().replaceFirstChar { it.titlecase() }) }
                )
            }
        }

        when (state.tab) {
            EditorTab.PREVIEW -> {
                Text(
                    text = "Preview position ${formatDuration(frame.positionMs)} / ${formatDuration(frame.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            EditorTab.VIDEOS -> {
                VideosBrowserPanel(
                    hasPermission = state.hasVideoPermission,
                    loading = state.libraryLoading,
                    error = state.libraryError,
                    videos = state.libraryVideos,
                    staged = state.stagedClips,
                    onRequestPermission = {
                        permissionLauncher.launch(viewModel.requiredVideoPermission())
                    },
                    onRefresh = { viewModel.loadLibraryVideos() },
                    onBrowseFolders = { folderVideoPicker.launch(arrayOf("video/*")) },
                    onMultiPick = { multiVideoPicker.launch(arrayOf("video/*")) },
                    onStageLibraryVideo = { viewModel.addLibraryVideoToStage(it) },
                    onRemoveStaged = viewModel::removeStagedClip,
                    onClearStaged = viewModel::clearStaged,
                    onCommitStaged = viewModel::commitStagedToTimeline
                )
            }

            EditorTab.EFFECTS -> {
                val selected = state.selectedClip
                val transition = state.document.transitions.firstOrNull { it.afterClipId == selected?.id }
                val hasNext = selected != null &&
                    state.document.clips.indexOfFirst { it.id == selected.id }
                        .let { it >= 0 && it < state.document.clips.lastIndex }
                EffectsPanel(
                    clip = selected,
                    transition = transition,
                    hasNextClip = hasNext,
                    transitionNote = state.transitionNote,
                    onVideoFades = viewModel::updateVideoFades,
                    onAudioFades = viewModel::updateAudioFades,
                    onTransition = viewModel::setTransitionAfterSelected
                )
            }

            EditorTab.TRIM -> {
                val clip = state.selectedClip
                if (clip == null) {
                    Text("Import a video to trim.")
                } else {
                    Text("Trim: ${formatDuration(clip.trimStartMs)} → ${formatDuration(clip.trimEndMs)}")
                    Text("Start")
                    Slider(
                        value = clip.trimStartMs.toFloat(),
                        onValueChange = {
                            viewModel.updateTrim(it.roundToLong(), clip.trimEndMs)
                        },
                        valueRange = 0f..clip.sourceDurationMs.toFloat()
                    )
                    Text("End")
                    Slider(
                        value = clip.trimEndMs.toFloat(),
                        onValueChange = {
                            viewModel.updateTrim(clip.trimStartMs, it.roundToLong())
                        },
                        valueRange = 0f..clip.sourceDurationMs.toFloat()
                    )
                }
            }

            EditorTab.AUDIO -> {
                AudioTrackPanel(
                    audio = state.document.audioTrack,
                    timelineMs = state.document.totalTimelineMs(),
                    onAddAudio = {
                        audioPicker.launch(
                            arrayOf("audio/*", "audio/mpeg", "audio/mp4", "audio/wav", "audio/x-wav")
                        )
                    },
                    onRemove = viewModel::removeAudioTrack,
                    onUpdate = { start, end, vol, offset ->
                        viewModel.updateAudioTrack(start, end, vol, offset)
                    }
                )
            }

            EditorTab.SUBTITLES -> {
                SubtitlesSection(
                    cues = state.document.subtitles,
                    onAdd = viewModel::addSubtitle,
                    onRemove = viewModel::removeSubtitle
                )
            }

            EditorTab.EXPORT -> {
                Text(
                    text = "Export includes fades, transitions, subtitles, and the added audio track mixed with original video audio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { viewModel.export() },
                    enabled = !state.isExporting && state.document.clips.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isExporting) "Exporting…" else "Export MP4")
                }
                state.lastExportPath?.let {
                    Text(
                        text = "Saved to:\n$it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        state.statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SubtitlesSection(
    cues: List<com.capcutmini.app.media.model.SubtitleCue>,
    onAdd: (String, Long, Long) -> Unit,
    onRemove: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var startSec by remember { mutableStateOf("0") }
    var endSec by remember { mutableStateOf("2") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Manual subtitles", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Subtitle text") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startSec,
                onValueChange = { startSec = it },
                label = { Text("Start s") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endSec,
                onValueChange = { endSec = it },
                label = { Text("End s") },
                modifier = Modifier.weight(1f)
            )
        }
        Button(
            onClick = {
                val start = ((startSec.toFloatOrNull() ?: 0f) * 1000f).toLong()
                val end = ((endSec.toFloatOrNull() ?: 2f) * 1000f).toLong()
                onAdd(text, start, end)
                text = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add subtitle")
        }
        cues.forEach { cue ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${cue.startMs / 1000f}-${cue.endMs / 1000f}s: ${cue.text}")
                TextButton(onClick = { onRemove(cue.id) }) { Text("Delete") }
            }
        }
    }
}
