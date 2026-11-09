package com.capcutmini.app.media.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextureOverlay
import com.capcutmini.app.media.model.SubtitleCue

/**
 * Burns manual subtitles into the export. Preview uses a Compose overlay instead.
 */
@UnstableApi
class SubtitleBurnOverlay(
    private val cues: List<SubtitleCue>,
    private val timelineOffsetMs: Long,
    private val width: Int = 1280,
    private val height: Int = 720
) : BitmapOverlay() {

    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = height * 0.055f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private var cachedText: String? = null
    private var cachedBitmap: Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val tMs = timelineOffsetMs + presentationTimeUs / 1000L
        val text = cues.firstOrNull { tMs in it.startMs until it.endMs }?.text.orEmpty()
        if (text != cachedText) {
            cachedText = text
            cachedBitmap.eraseColor(Color.TRANSPARENT)
            if (text.isNotBlank()) {
                val canvas = Canvas(cachedBitmap)
                val layout = StaticLayout.Builder.obtain(
                    text, 0, text.length, paint, (width * 0.9f).toInt()
                )
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                    .build()
                canvas.save()
                val x = (width - layout.width) / 2f
                val y = height * 0.82f - layout.height / 2f
                canvas.translate(x, y)
                layout.draw(canvas)
                canvas.restore()
            }
        }
        return cachedBitmap
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val tMs = timelineOffsetMs + presentationTimeUs / 1000L
        val visible = cues.any { tMs in it.startMs until it.endMs }
        return OverlaySettings.Builder()
            .setAlphaScale(if (visible) 1f else 0f)
            .build()
    }

    companion object {
        fun asTextureOverlays(
            cues: List<SubtitleCue>,
            timelineOffsetMs: Long
        ): List<TextureOverlay> {
            if (cues.isEmpty()) return emptyList()
            return listOf(SubtitleBurnOverlay(cues, timelineOffsetMs))
        }
    }
}
