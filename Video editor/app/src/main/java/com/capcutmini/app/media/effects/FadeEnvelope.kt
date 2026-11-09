package com.capcutmini.app.media.effects

/**
 * Shared linear gain / luminance envelope used by preview, audio processor, and RgbMatrix.
 * 0 = silence/black, 1 = full level.
 */
object FadeEnvelope {
    fun gain(t: Long, duration: Long, fadeIn: Long, fadeOut: Long): Float {
        if (duration <= 0L) return 1f
        val clamped = t.coerceIn(0L, duration)
        var value = 1f
        if (fadeIn > 0L && clamped < fadeIn) {
            value = clamped.toFloat() / fadeIn.toFloat()
        }
        if (fadeOut > 0L && clamped > duration - fadeOut) {
            value = minOf(value, (duration - clamped).toFloat() / fadeOut.toFloat())
        }
        return value.coerceIn(0f, 1f)
    }
}
