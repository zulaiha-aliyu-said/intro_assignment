package com.capcutmini.app.media.model

import com.capcutmini.app.media.effects.FadeEnvelope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FadeLimitsTest {

    @Test
    fun envelope_startsBlackSilent_andEndsBlackSilent() {
        assertEquals(0f, FadeEnvelope.gain(0, 10_000, 1_000, 1_000), 0.001f)
        assertEquals(1f, FadeEnvelope.gain(5_000, 10_000, 1_000, 1_000), 0.001f)
        assertEquals(0f, FadeEnvelope.gain(10_000, 10_000, 1_000, 1_000), 0.001f)
        assertEquals(0.5f, FadeEnvelope.gain(500, 10_000, 1_000, 1_000), 0.001f)
    }

    @Test
    fun sanitize_clampsToSupportedRange() {
        assertEquals(0L, FadeLimits.sanitize(0))
        assertEquals(0L, FadeLimits.sanitize(-10))
        assertEquals(500L, FadeLimits.sanitize(100))
        assertEquals(3000L, FadeLimits.sanitize(5000))
        assertEquals(1500L, FadeLimits.sanitize(1500))
    }

    @Test
    fun clampPair_fitsShortClips() {
        val (inn, out) = FadeLimits.clampPair(2000, 2000, 1500)
        assertTrue(inn + out <= 1500)
        assertTrue(inn >= 0 && out >= 0)
    }

    @Test
    fun resolve_appliesFadeThroughBlackTransition() {
        val a = ClipEdit(
            id = "a",
            uri = "content://a",
            displayName = "A",
            sourceDurationMs = 10_000,
            videoFadeInMs = 0,
            videoFadeOutMs = 0,
            audioFadeInMs = 0,
            audioFadeOutMs = 0
        )
        val b = ClipEdit(
            id = "b",
            uri = "content://b",
            displayName = "B",
            sourceDurationMs = 10_000
        )
        val doc = EditDocument(
            clips = listOf(a, b),
            transitions = listOf(
                TransitionEdit("a", TransitionType.FADE_THROUGH_BLACK, 1000)
            )
        )
        val resolved = doc.resolveClipEffects()
        assertEquals(1000L, resolved[0].videoFadeOutMs)
        assertEquals(1000L, resolved[1].videoFadeInMs)
        assertEquals(1000L, resolved[0].audioFadeOutMs)
        assertEquals(1000L, resolved[1].audioFadeInMs)
    }
}
