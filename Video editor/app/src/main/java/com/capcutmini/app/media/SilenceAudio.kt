package com.capcutmini.app.media

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Creates a temporary mono 16-bit PCM WAV of silence for timeline start-offset gaps. */
object SilenceAudio {
    fun fileForDuration(context: Context, durationMs: Long): File {
        val safeMs = durationMs.coerceAtLeast(50L)
        val out = File(context.cacheDir, "silence_${safeMs}ms.wav")
        if (out.exists() && out.length() > 44L) return out
        val sampleRate = 44_100
        val channels = 1
        val bitsPerSample = 16
        val totalSamples = ((sampleRate * safeMs) / 1000L).toInt().coerceAtLeast(1)
        val dataSize = totalSamples * channels * (bitsPerSample / 8)
        FileOutputStream(out).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1) // PCM
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * channels * bitsPerSample / 8)
            header.putShort((channels * bitsPerSample / 8).toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            fos.write(header.array())
            fos.write(ByteArray(dataSize))
        }
        return out
    }
}
