package com.capcutmini.app.media.effects

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * PCM gain envelope for fade-in from silence and fade-out to silence.
 * Tracks processed frames so A/V stay aligned with [VideoFadeRgbMatrix] on the same clip.
 */
@UnstableApi
class AudioFadeProcessor(
    private val fadeInUs: Long,
    private val fadeOutUs: Long,
    private val durationUs: Long
) : BaseAudioProcessor() {

    private var framesWritten: Long = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val format = outputAudioFormat
        val bytesPerFrame = format.channelCount * 2 // 16-bit PCM
        if (bytesPerFrame <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val remaining = inputBuffer.remaining()
        val output = replaceOutputBuffer(remaining)
        val sampleRate = format.sampleRate.toLong().coerceAtLeast(1L)

        while (inputBuffer.hasRemaining()) {
            val frameIndex = framesWritten
            val timeUs = frameIndex * 1_000_000L / sampleRate
            val gain = FadeEnvelope.gain(timeUs, durationUs, fadeInUs, fadeOutUs)

            var sumSquares = 0.0
            for (ch in 0 until format.channelCount) {
                if (!inputBuffer.hasRemaining()) break
                val sample = inputBuffer.short
                val scaled = (sample * gain).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                output.putShort(scaled.toShort())
                sumSquares += scaled * scaled.toDouble()
            }
            // Soft protection: if somehow over unity after multi-channel, already clamped per sample.
            framesWritten++
            if (sumSquares.isNaN()) {
                // no-op; keeps lint quiet on unused in some AGP versions
            }
        }
        output.flip()
    }

    override fun onFlush() {
        framesWritten = 0L
    }

    override fun onReset() {
        framesWritten = 0L
    }

}
