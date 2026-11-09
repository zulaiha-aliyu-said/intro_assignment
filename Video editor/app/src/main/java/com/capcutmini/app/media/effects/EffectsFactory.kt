package com.capcutmini.app.media.effects

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.capcutmini.app.media.SilenceAudio
import com.capcutmini.app.media.model.AudioTrackEdit
import com.capcutmini.app.media.model.EditDocument
import com.capcutmini.app.media.model.ResolvedClipEffects
import com.capcutmini.app.media.model.resolveClipEffects
import com.google.common.collect.ImmutableList

@UnstableApi
object EffectsFactory {

    fun buildComposition(context: Context, document: EditDocument): Composition {
        val videoItems = buildEditedMediaItems(document)
        val videoSequence = EditedMediaItemSequence(videoItems)
        val audio = document.audioTrack?.sanitized()
        if (audio == null) {
            return Composition.Builder(videoSequence).build()
        }
        val audioItems = buildBackgroundAudioSequence(context, audio)
        val audioSequence = EditedMediaItemSequence(audioItems)
        // Concurrent sequences: Transformer mixes audio streams with the video track audio.
        return Composition.Builder(videoSequence, audioSequence).build()
    }

    fun buildEditedMediaItems(document: EditDocument): List<EditedMediaItem> {
        val resolved = document.resolveClipEffects()
        var timelineOffsetMs = 0L
        return resolved.map { item ->
            val edited = buildEditedItem(item, document, timelineOffsetMs)
            timelineOffsetMs += item.clip.trimmedDurationMs
            edited
        }
    }

    fun buildBackgroundAudioSequence(
        context: Context,
        audio: AudioTrackEdit
    ): List<EditedMediaItem> {
        val track = audio.sanitized()
        val items = mutableListOf<EditedMediaItem>()
        if (track.startOffsetMs > 0L) {
            val silenceFile = SilenceAudio.fileForDuration(context, track.startOffsetMs)
            val silenceDurationUs = track.startOffsetMs * 1000L
            items += EditedMediaItem.Builder(
                MediaItem.fromUri(Uri.fromFile(silenceFile))
            )
                .setDurationUs(silenceDurationUs)
                .setRemoveVideo(true)
                .build()
        }
        val durationUs = track.trimmedDurationMs * 1000L
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(track.uri))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(track.trimStartMs)
                    .setEndPositionMs(track.trimEndMs)
                    .build()
            )
            .build()
        val processors = buildList<AudioProcessor> {
            if (track.volume != 1f) {
                add(VolumeScaleAudioProcessor(track.volume))
            }
        }
        items += EditedMediaItem.Builder(mediaItem)
            .setDurationUs(durationUs)
            .setRemoveVideo(true)
            .setEffects(Effects(processors, emptyList()))
            .build()
        return items
    }

    private fun buildEditedItem(
        resolved: ResolvedClipEffects,
        document: EditDocument,
        timelineOffsetMs: Long
    ): EditedMediaItem {
        val clip = resolved.clip
        val durationUs = clip.trimmedDurationMs * 1000L
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(clip.uri))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs)
                    .setEndPositionMs(clip.trimEndMs)
                    .build()
            )
            .build()

        val videoEffects = buildList {
            if (resolved.videoFadeInMs > 0L || resolved.videoFadeOutMs > 0L) {
                add(
                    VideoFadeRgbMatrix(
                        fadeInUs = resolved.videoFadeInMs * 1000L,
                        fadeOutUs = resolved.videoFadeOutMs * 1000L,
                        durationUs = durationUs
                    )
                )
            }
            val overlays = SubtitleBurnOverlay.asTextureOverlays(
                cues = document.subtitles,
                timelineOffsetMs = timelineOffsetMs
            )
            if (overlays.isNotEmpty()) {
                add(OverlayEffect(ImmutableList.copyOf(overlays)))
            }
        }

        val audioProcessors = buildList<AudioProcessor> {
            if (resolved.audioFadeInMs > 0L || resolved.audioFadeOutMs > 0L) {
                add(
                    AudioFadeProcessor(
                        fadeInUs = resolved.audioFadeInMs * 1000L,
                        fadeOutUs = resolved.audioFadeOutMs * 1000L,
                        durationUs = durationUs
                    )
                )
            }
        }

        return EditedMediaItem.Builder(mediaItem)
            .setDurationUs(durationUs)
            .setEffects(Effects(audioProcessors, videoEffects))
            .build()
    }

    fun outputMimeType(): String = MimeTypes.VIDEO_H264
}
