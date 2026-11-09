package com.capcutmini.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.capcutmini.app.media.model.AudioTrackEdit
import com.capcutmini.app.ui.components.formatDuration
import kotlin.math.roundToLong

@Composable
fun AudioTrackPanel(
    audio: AudioTrackEdit?,
    timelineMs: Long,
    onAddAudio: () -> Unit,
    onRemove: () -> Unit,
    onUpdate: (trimStart: Long?, trimEnd: Long?, volume: Float?, startOffset: Long?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Audio track", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Add MP3 / WAV / M4A. Mixed with the video’s original audio on export via Media3 Transformer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = onAddAudio, modifier = Modifier.fillMaxWidth()) {
            Text(if (audio == null) "Add Audio" else "Replace Audio")
        }

        if (audio == null) {
            Text(
                "No audio track yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        Text(audio.displayName, style = MaterialTheme.typography.titleLarge)
        Text(
            "Duration ${formatDuration(audio.trimmedDurationMs)} / source ${formatDuration(audio.sourceDurationMs)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Volume ${(audio.volume * 100).toInt()}%")
        Slider(
            value = audio.volume,
            onValueChange = { onUpdate(null, null, it, null) },
            valueRange = 0f..2f
        )

        Text("Start on timeline: ${formatDuration(audio.startOffsetMs)}")
        val maxOffset = timelineMs.coerceAtLeast(0L).toFloat()
        Slider(
            value = audio.startOffsetMs.toFloat().coerceAtMost(maxOffset),
            onValueChange = { onUpdate(null, null, null, it.roundToLong()) },
            valueRange = 0f..maxOf(maxOffset, 1f)
        )

        Text("Trim start: ${formatDuration(audio.trimStartMs)}")
        Slider(
            value = audio.trimStartMs.toFloat(),
            onValueChange = { onUpdate(it.roundToLong(), audio.trimEndMs, null, null) },
            valueRange = 0f..audio.sourceDurationMs.toFloat()
        )
        Text("Trim end: ${formatDuration(audio.trimEndMs)}")
        Slider(
            value = audio.trimEndMs.toFloat(),
            onValueChange = { onUpdate(audio.trimStartMs, it.roundToLong(), null, null) },
            valueRange = 0f..audio.sourceDurationMs.toFloat()
        )

        OutlinedButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
            Text("Remove audio track")
        }
    }
}
