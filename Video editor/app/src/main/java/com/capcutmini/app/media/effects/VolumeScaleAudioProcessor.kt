package com.capcutmini.app.media.effects

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/** Scales 16-bit PCM by [volume] (0..2). Used for export mix of the added audio track. */
@UnstableApi
class VolumeScaleAudioProcessor(
    private val volume: Float
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val remaining = inputBuffer.remaining()
        val output = replaceOutputBuffer(remaining)
        val gain = volume.coerceIn(0f, 2f)
        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short
            val scaled = (sample * gain).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.putShort(scaled.toShort())
        }
        output.flip()
    }
}
