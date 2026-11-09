package com.capcutmini.app.media.effects

import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix

/**
 * Time-varying RGB scale matrix for fade-from-black / fade-to-black.
 * scale=0 → black frame, scale=1 → original picture. Applied by Media3 Transformer.
 */
@UnstableApi
class VideoFadeRgbMatrix(
    private val fadeInUs: Long,
    private val fadeOutUs: Long,
    private val durationUs: Long
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val scale = FadeEnvelope.gain(presentationTimeUs, durationUs, fadeInUs, fadeOutUs)
        return floatArrayOf(
            scale, 0f, 0f, 0f,
            0f, scale, 0f, 0f,
            0f, 0f, scale, 0f,
            0f, 0f, 0f, 1f
        )
    }
}
