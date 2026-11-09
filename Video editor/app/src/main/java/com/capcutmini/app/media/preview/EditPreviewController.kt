package com.capcutmini.app.media.preview

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.capcutmini.app.media.effects.FadeEnvelope
import com.capcutmini.app.media.model.EditDocument
import com.capcutmini.app.media.model.ResolvedClipEffects
import com.capcutmini.app.media.model.resolveClipEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreviewFrameState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val videoBlackAlpha: Float = 0f,
    val activeSubtitle: String? = null,
    val clipIndex: Int = 0
)

/**
 * Preview uses ExoPlayer playlist of clipped video items.
 * Optional background audio uses a second ExoPlayer mixed under the video
 * (same track that Transformer mixes on export).
 */
class EditPreviewController(
    context: Context
) {
    private val appContext = context.applicationContext

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = false
    }

    private val audioPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = false
        volume = 1f
    }

    private val _frame = MutableStateFlow(PreviewFrameState())
    val frame: StateFlow<PreviewFrameState> = _frame.asStateFlow()

    private var resolved: List<ResolvedClipEffects> = emptyList()
    private var document: EditDocument = EditDocument()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateFrame()
            syncBackgroundAudio()
            handler.postDelayed(this, 33L)
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateFrame()
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
            ) {
                syncBackgroundAudio()
            }
        }
    }

    init {
        player.addListener(listener)
        handler.post(tick)
    }

    fun setDocument(doc: EditDocument) {
        document = doc
        resolved = doc.resolveClipEffects()
        val mediaItems = resolved.map { item ->
            MediaItem.Builder()
                .setUri(Uri.parse(item.clip.uri))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(item.clip.trimStartMs)
                        .setEndPositionMs(item.clip.trimEndMs)
                        .build()
                )
                .build()
        }
        val wasPlaying = player.isPlaying
        val pos = player.currentPosition
        player.setMediaItems(mediaItems, /* resetPosition= */ true)
        player.prepare()
        if (mediaItems.isNotEmpty()) {
            player.seekTo(
                0,
                minOf(pos, resolved.firstOrNull()?.clip?.trimmedDurationMs ?: 0L)
            )
            player.playWhenReady = wasPlaying
        }

        val audio = doc.audioTrack?.sanitized()
        if (audio == null) {
            audioPlayer.stop()
            audioPlayer.clearMediaItems()
        } else {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(audio.uri))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(audio.trimStartMs)
                        .setEndPositionMs(audio.trimEndMs)
                        .build()
                )
                .build()
            audioPlayer.setMediaItem(item)
            audioPlayer.prepare()
            audioPlayer.volume = audio.volume.coerceIn(0f, 2f)
            audioPlayer.playWhenReady = false
        }
        updateFrame()
        syncBackgroundAudio()
    }

    private fun syncBackgroundAudio() {
        val audio = document.audioTrack?.sanitized() ?: run {
            if (audioPlayer.isPlaying) audioPlayer.pause()
            return
        }
        val timelineMs = _frame.value.positionMs
        val audioStart = audio.startOffsetMs
        val audioEnd = audioStart + audio.trimmedDurationMs
        val shouldPlay = player.isPlaying && timelineMs in audioStart until audioEnd
        if (!shouldPlay) {
            if (audioPlayer.isPlaying) audioPlayer.pause()
            return
        }
        val posInAudio = (timelineMs - audioStart).coerceAtLeast(0L)
        if (kotlin.math.abs(audioPlayer.currentPosition - posInAudio) > 120L) {
            audioPlayer.seekTo(posInAudio)
        }
        audioPlayer.volume = audio.volume.coerceIn(0f, 2f)
        if (!audioPlayer.isPlaying) {
            audioPlayer.playWhenReady = true
            audioPlayer.play()
        }
    }

    private fun updateFrame() {
        if (resolved.isEmpty()) {
            player.volume = 1f
            _frame.value = PreviewFrameState()
            return
        }
        val index = player.currentMediaItemIndex.coerceIn(0, resolved.lastIndex)
        val item = resolved[index]
        val posInClip = player.currentPosition.coerceAtLeast(0L)
        val clipDur = item.clip.trimmedDurationMs.coerceAtLeast(1L)

        val videoGain = FadeEnvelope.gain(posInClip, clipDur, item.videoFadeInMs, item.videoFadeOutMs)
        val audioGain = FadeEnvelope.gain(posInClip, clipDur, item.audioFadeInMs, item.audioFadeOutMs)
        player.volume = audioGain

        var timelineMs = posInClip
        for (i in 0 until index) {
            timelineMs += resolved[i].clip.trimmedDurationMs
        }
        val subtitle = document.subtitles.firstOrNull { timelineMs in it.startMs until it.endMs }?.text

        var total = 0L
        resolved.forEach { total += it.clip.trimmedDurationMs }

        _frame.value = PreviewFrameState(
            positionMs = timelineMs,
            durationMs = total,
            videoBlackAlpha = 1f - videoGain,
            activeSubtitle = subtitle,
            clipIndex = index
        )
    }

    fun release() {
        handler.removeCallbacks(tick)
        player.removeListener(listener)
        player.release()
        audioPlayer.release()
    }
}
