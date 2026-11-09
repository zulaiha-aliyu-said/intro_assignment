package com.capcutmini.app.ui.editor

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.capcutmini.app.data.repository.ProjectRepository
import com.capcutmini.app.media.MediaMetadataReader
import com.capcutmini.app.media.export.ExportOutcome
import com.capcutmini.app.media.export.VideoExporter
import com.capcutmini.app.media.library.DeviceVideo
import com.capcutmini.app.media.library.LocalVideoLibrary
import com.capcutmini.app.media.model.AudioTrackEdit
import com.capcutmini.app.media.model.ClipEdit
import com.capcutmini.app.media.model.EditDocument
import com.capcutmini.app.media.model.FadeLimits
import com.capcutmini.app.media.model.SubtitleCue
import com.capcutmini.app.media.model.TransitionEdit
import com.capcutmini.app.media.model.TransitionType
import com.capcutmini.app.media.model.rebuildTransitions
import com.capcutmini.app.media.preview.EditPreviewController
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorTab { PREVIEW, VIDEOS, EFFECTS, TRIM, AUDIO, SUBTITLES, EXPORT }

data class StagedClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val displayName: String,
    val durationMs: Long
)

data class EditorUiState(
    val projectId: Long? = null,
    val document: EditDocument = EditDocument(),
    val selectedClipId: String? = null,
    val stagedClips: List<StagedClip> = emptyList(),
    val libraryVideos: List<DeviceVideo> = emptyList(),
    val libraryLoading: Boolean = false,
    val libraryError: String? = null,
    val hasVideoPermission: Boolean = false,
    val tab: EditorTab = EditorTab.PREVIEW,
    val statusMessage: String? = null,
    val isExporting: Boolean = false,
    val lastExportPath: String? = null,
    val transitionNote: String = CROSS_DISSOLVE_NOTE
) {
    val selectedClip: ClipEdit?
        get() = document.clips.firstOrNull { it.id == selectedClipId }
            ?: document.clips.firstOrNull()
}

private const val CROSS_DISSOLVE_NOTE =
    "Media3 1.5.1 has no native overlapping cross-dissolve on a single sequence. " +
        "\"Cross-dissolve\" uses a real fade-through-black transition in Transformer export."

@OptIn(UnstableApi::class)
class EditorViewModel(
    application: Application,
    private val repository: ProjectRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val previewController = EditPreviewController(application)
    private val exporter = VideoExporter(application)
    private val videoLibrary = LocalVideoLibrary(application)

    private var persistJob: Job? = null

    init {
        refreshPermissionState()
    }

    fun selectTab(tab: EditorTab) {
        _uiState.update { it.copy(tab = tab) }
        if (tab == EditorTab.VIDEOS) {
            refreshPermissionState()
            if (_uiState.value.hasVideoPermission) loadLibraryVideos()
        }
    }

    fun selectClip(clipId: String) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun refreshPermissionState() {
        val granted = hasReadVideoPermission()
        _uiState.update { it.copy(hasVideoPermission = granted) }
    }

    fun onVideoPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasVideoPermission = granted,
                libraryError = if (granted) null else "Permission denied. Use Browse folders to pick videos."
            )
        }
        if (granted) loadLibraryVideos()
    }

    fun loadLibraryVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(libraryLoading = true, libraryError = null) }
            val result = videoLibrary.loadAllVideos()
            result.fold(
                onSuccess = { videos ->
                    _uiState.update {
                        it.copy(
                            libraryVideos = videos,
                            libraryLoading = false,
                            libraryError = if (videos.isEmpty()) {
                                "No videos found on this device. Try Browse folders."
                            } else null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            libraryLoading = false,
                            libraryError = err.message ?: "Could not load videos"
                        )
                    }
                }
            )
        }
    }

    fun stageUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val staged = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    val name = MediaMetadataReader.displayName(getApplication(), uri) ?: "Video"
                    val duration = MediaMetadataReader.durationMs(getApplication(), uri)
                        .coerceAtLeast(1_000L)
                    StagedClip(uri = uri, displayName = name, durationMs = duration)
                }
            }
            _uiState.update {
                it.copy(
                    stagedClips = it.stagedClips + staged,
                    statusMessage = "Staged ${staged.size} clip(s). Remove any, then Add to timeline."
                )
            }
        }
    }

    fun removeStagedClip(id: String) {
        _uiState.update { it.copy(stagedClips = it.stagedClips.filterNot { c -> c.id == id }) }
    }

    fun clearStaged() {
        _uiState.update { it.copy(stagedClips = emptyList()) }
    }

    fun commitStagedToTimeline() {
        val staged = _uiState.value.stagedClips
        if (staged.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Stage videos first.") }
            return
        }
        val current = _uiState.value.document
        val newClips = staged.map {
            ClipEdit(
                uri = it.uri.toString(),
                displayName = it.displayName,
                sourceDurationMs = it.durationMs,
                trimEndMs = it.durationMs
            )
        }
        val clips = current.clips + newClips
        val doc = current.copy(
            projectTitle = if (current.clips.isEmpty()) {
                newClips.first().displayName
            } else current.projectTitle,
            clips = clips,
            transitions = rebuildTransitions(clips, current.transitions)
        )
        applyDocument(doc, selectId = newClips.last().id)
        viewModelScope.launch { ensureProject(doc) }
        _uiState.update {
            it.copy(
                stagedClips = emptyList(),
                statusMessage = "Added ${newClips.size} clip(s) to timeline"
            )
        }
    }

    fun addLibraryVideoToStage(video: DeviceVideo) {
        stageUris(listOf(video.uri))
    }

    fun removeClip(clipId: String) {
        val current = _uiState.value.document
        val clips = current.clips.filterNot { it.id == clipId }
        val doc = current.copy(
            clips = clips,
            transitions = rebuildTransitions(clips, current.transitions)
        )
        applyDocument(doc)
        persistSoon()
    }

    fun moveClip(clipId: String, delta: Int) {
        val clips = _uiState.value.document.clips.toMutableList()
        val index = clips.indexOfFirst { it.id == clipId }
        val target = index + delta
        if (index < 0 || target !in clips.indices) return
        val item = clips.removeAt(index)
        clips.add(target, item)
        val current = _uiState.value.document
        val doc = current.copy(
            clips = clips,
            transitions = rebuildTransitions(clips, current.transitions)
        )
        applyDocument(doc, selectId = clipId)
        persistSoon()
    }

    fun onVideoPicked(uri: Uri, displayName: String?, append: Boolean) {
        // Kept for compatibility; routes through staging then immediate commit.
        viewModelScope.launch {
            val duration = withContext(Dispatchers.IO) {
                MediaMetadataReader.durationMs(getApplication(), uri)
            }.coerceAtLeast(1_000L)
            val name = displayName?.takeIf { it.isNotBlank() } ?: "Clip"
            val staged = StagedClip(uri = uri, displayName = name, durationMs = duration)
            _uiState.update { it.copy(stagedClips = listOf(staged)) }
            if (!append && _uiState.value.document.clips.isNotEmpty()) {
                // Replace timeline when using primary "Select videos" without append intent:
                // still append via commit; use explicit replace only when empty staging replace.
            }
            commitStagedToTimeline()
            if (!append) {
                // If user meant replace: keep behavior as "import as first set" when timeline empty
                // already handled by commit append. Primary multi-select always appends after stage.
            }
        }
    }

    fun setAudioTrack(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            val duration = withContext(Dispatchers.IO) {
                MediaMetadataReader.durationMs(getApplication(), uri)
            }.coerceAtLeast(500L)
            val name = displayName?.takeIf { it.isNotBlank() } ?: "Audio"
            val track = AudioTrackEdit(
                uri = uri.toString(),
                displayName = name,
                sourceDurationMs = duration,
                trimEndMs = duration
            ).sanitized()
            val doc = _uiState.value.document.copy(audioTrack = track)
            applyDocument(doc)
            persistSoon()
            _uiState.update {
                it.copy(
                    tab = EditorTab.AUDIO,
                    statusMessage = "Audio track added: $name"
                )
            }
        }
    }

    fun removeAudioTrack() {
        val doc = _uiState.value.document.copy(audioTrack = null)
        applyDocument(doc)
        persistSoon()
        _uiState.update { it.copy(statusMessage = "Audio track removed") }
    }

    fun updateAudioTrack(
        trimStartMs: Long? = null,
        trimEndMs: Long? = null,
        volume: Float? = null,
        startOffsetMs: Long? = null
    ) {
        val current = _uiState.value.document.audioTrack ?: return
        val updated = current.copy(
            trimStartMs = trimStartMs ?: current.trimStartMs,
            trimEndMs = trimEndMs ?: current.trimEndMs,
            volume = volume ?: current.volume,
            startOffsetMs = startOffsetMs ?: current.startOffsetMs
        ).sanitized()
        applyDocument(_uiState.value.document.copy(audioTrack = updated))
        persistSoon()
    }

    fun updateVideoFades(fadeInMs: Long, fadeOutMs: Long) {
        updateSelectedClip {
            it.copy(
                videoFadeInMs = FadeLimits.sanitize(fadeInMs),
                videoFadeOutMs = FadeLimits.sanitize(fadeOutMs)
            ).withClampedFades()
        }
    }

    fun updateAudioFades(fadeInMs: Long, fadeOutMs: Long) {
        updateSelectedClip {
            it.copy(
                audioFadeInMs = FadeLimits.sanitize(fadeInMs),
                audioFadeOutMs = FadeLimits.sanitize(fadeOutMs)
            ).withClampedFades()
        }
    }

    fun updateTrim(startMs: Long, endMs: Long) {
        updateSelectedClip { clip ->
            val start = startMs.coerceIn(0L, clip.sourceDurationMs)
            val end = endMs.coerceIn(start + 200L, clip.sourceDurationMs)
            clip.copy(trimStartMs = start, trimEndMs = end).withClampedFades()
        }
    }

    fun setTransitionAfterSelected(type: TransitionType, durationMs: Long) {
        val clip = _uiState.value.selectedClip ?: return
        val clips = _uiState.value.document.clips
        val index = clips.indexOfFirst { it.id == clip.id }
        if (index < 0 || index >= clips.lastIndex) {
            _uiState.update {
                it.copy(statusMessage = "Transitions apply between consecutive clips — add another clip first.")
            }
            return
        }
        val duration = FadeLimits.sanitize(durationMs).let { if (it == 0L) FadeLimits.MIN_MS else it }
        val transitions = _uiState.value.document.transitions
            .filterNot { it.afterClipId == clip.id } +
            TransitionEdit(afterClipId = clip.id, type = type, durationMs = duration)
        applyDocument(_uiState.value.document.copy(transitions = transitions))
        persistSoon()
    }

    fun addSubtitle(text: String, startMs: Long, endMs: Long) {
        if (text.isBlank()) return
        val cue = SubtitleCue(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            startMs = startMs.coerceAtLeast(0L),
            endMs = endMs.coerceAtLeast(startMs + 200L)
        )
        applyDocument(
            _uiState.value.document.copy(subtitles = _uiState.value.document.subtitles + cue)
        )
        persistSoon()
    }

    fun removeSubtitle(id: String) {
        applyDocument(
            _uiState.value.document.copy(
                subtitles = _uiState.value.document.subtitles.filterNot { it.id == id }
            )
        )
        persistSoon()
    }

    fun export() {
        val doc = _uiState.value.document
        if (doc.clips.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Import a video before exporting.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = "Exporting…") }
            val result = withContext(Dispatchers.Main) {
                exporter.export(doc)
            }
            when (result) {
                is ExportOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            lastExportPath = result.outputFile.absolutePath,
                            statusMessage = "Exported: ${result.outputFile.name}"
                        )
                    }
                }
                is ExportOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            statusMessage = "Export failed: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    fun requiredVideoPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun hasReadVideoPermission(): Boolean {
        val permission = requiredVideoPermission()
        return ContextCompat.checkSelfPermission(
            getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateSelectedClip(transform: (ClipEdit) -> ClipEdit) {
        val selectedId = _uiState.value.selectedClip?.id ?: return
        val clips = _uiState.value.document.clips.map { clip ->
            if (clip.id == selectedId) transform(clip) else clip
        }
        applyDocument(_uiState.value.document.copy(clips = clips), selectId = selectedId)
        persistSoon()
    }

    private fun applyDocument(doc: EditDocument, selectId: String? = _uiState.value.selectedClipId) {
        previewController.setDocument(doc)
        _uiState.update {
            it.copy(
                document = doc,
                selectedClipId = selectId ?: doc.clips.firstOrNull()?.id
            )
        }
    }

    private suspend fun ensureProject(doc: EditDocument) {
        val duration = doc.totalTimelineMs()
        val existingId = _uiState.value.projectId
        if (existingId == null) {
            val id = repository.createProject(
                title = doc.projectTitle,
                sourceUri = doc.clips.first().uri,
                durationMs = duration,
                editDocument = doc
            )
            _uiState.update { it.copy(projectId = id) }
        } else {
            repository.saveEditDocument(existingId, doc, duration)
        }
    }

    private fun persistSoon() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            val state = _uiState.value
            val id = state.projectId ?: return@launch
            repository.saveEditDocument(id, state.document, state.document.totalTimelineMs())
        }
    }

    override fun onCleared() {
        previewController.release()
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val repository: ProjectRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EditorViewModel::class.java))
            return EditorViewModel(application, repository) as T
        }
    }
}
