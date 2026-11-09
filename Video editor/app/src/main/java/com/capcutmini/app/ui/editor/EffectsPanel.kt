package com.capcutmini.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.capcutmini.app.media.model.ClipEdit
import com.capcutmini.app.media.model.FadeLimits
import com.capcutmini.app.media.model.TransitionEdit
import com.capcutmini.app.media.model.TransitionType
import kotlin.math.roundToInt

@Composable
fun EffectsPanel(
    clip: ClipEdit?,
    transition: TransitionEdit?,
    hasNextClip: Boolean,
    transitionNote: String,
    onVideoFades: (Long, Long) -> Unit,
    onAudioFades: (Long, Long) -> Unit,
    onTransition: (TransitionType, Long) -> Unit
) {
    if (clip == null) {
        Text(
            text = "Import a video to configure fades and transitions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Effects / Transitions", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Clip: ${clip.displayName} (${clip.trimmedDurationMs / 1000f}s trimmed)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FadeSliderGroup(
            title = "Video fade in (from black)",
            valueMs = clip.videoFadeInMs,
            onChange = { onVideoFades(it, clip.videoFadeOutMs) }
        )
        FadeSliderGroup(
            title = "Video fade out (to black)",
            valueMs = clip.videoFadeOutMs,
            onChange = { onVideoFades(clip.videoFadeInMs, it) }
        )
        FadeSliderGroup(
            title = "Audio fade in (from silence)",
            valueMs = clip.audioFadeInMs,
            onChange = { onAudioFades(it, clip.audioFadeOutMs) }
        )
        FadeSliderGroup(
            title = "Audio fade out (to silence)",
            valueMs = clip.audioFadeOutMs,
            onChange = { onAudioFades(clip.audioFadeInMs, it) }
        )

        Text("Transition to next clip", style = MaterialTheme.typography.titleLarge)
        if (!hasNextClip) {
            Text(
                text = "Add a second clip to enable transitions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val selected = transition?.type ?: TransitionType.NONE
            val duration = transition?.durationMs ?: FadeLimits.MIN_MS
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TransitionChip("None", selected == TransitionType.NONE) {
                    onTransition(TransitionType.NONE, duration)
                }
                TransitionChip("Fade black", selected == TransitionType.FADE_THROUGH_BLACK) {
                    onTransition(TransitionType.FADE_THROUGH_BLACK, duration)
                }
                TransitionChip("Cross*", selected == TransitionType.CROSS_DISSOLVE_FALLBACK) {
                    onTransition(TransitionType.CROSS_DISSOLVE_FALLBACK, duration)
                }
            }
            FadeSliderGroup(
                title = "Transition duration",
                valueMs = duration,
                allowZero = false,
                onChange = { onTransition(selected, it) }
            )
            Text(
                text = transitionNote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TransitionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun FadeSliderGroup(
    title: String,
    valueMs: Long,
    allowZero: Boolean = true,
    onChange: (Long) -> Unit
) {
    val seconds = if (valueMs <= 0L && allowZero) 0f else (valueMs.coerceAtLeast(FadeLimits.MIN_MS) / 1000f)
    Column {
        Text(
            text = "$title: ${"%.1f".format(seconds)}s",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = seconds,
            onValueChange = { v ->
                if (allowZero && v < 0.25f) {
                    onChange(0L)
                } else {
                    val ms = (v.coerceIn(0.5f, 3f) * 1000f).roundToInt().toLong()
                    onChange(FadeLimits.sanitize(ms))
                }
            },
            valueRange = if (allowZero) 0f..3f else 0.5f..3f
        )
    }
}
