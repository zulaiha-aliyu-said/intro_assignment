package com.capcutmini.app.media.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditDocumentAudioTest {

  @Test
  fun audioTrack_trimAndVolumeOnDocument() {
    val audio =
      AudioTrackEdit(
          uri = "content://audio/1",
          displayName = "song.mp3",
          sourceDurationMs = 12_000,
          trimStartMs = 1_000,
          trimEndMs = 8_000,
          volume = 0.75f,
          startOffsetMs = 500
        )
        .sanitized()
    val doc =
      EditDocument(
        clips =
          listOf(
            ClipEdit(
              uri = "content://video/1",
              displayName = "v.mp4",
              sourceDurationMs = 10_000
            )
          ),
        audioTrack = audio
      )
    assertNotNull(doc.audioTrack)
    assertEquals("song.mp3", doc.audioTrack!!.displayName)
    assertEquals(0.75f, doc.audioTrack!!.volume, 0.001f)
    assertEquals(7_000L, doc.audioTrack!!.trimmedDurationMs)
    assertEquals(10_000L, doc.totalTimelineMs())
  }

  @Test
  fun rebuildTransitions_preservesOrderAfterReorder() {
    val a = ClipEdit(id = "a", uri = "a", displayName = "A", sourceDurationMs = 5_000)
    val b = ClipEdit(id = "b", uri = "b", displayName = "B", sourceDurationMs = 5_000)
    val c = ClipEdit(id = "c", uri = "c", displayName = "C", sourceDurationMs = 5_000)
    val existing =
      listOf(
        TransitionEdit("a", TransitionType.FADE_THROUGH_BLACK, 1000),
        TransitionEdit("b", TransitionType.NONE, 500)
      )
    val reordered = listOf(c, a, b)
    val rebuilt = rebuildTransitions(reordered, existing)
    assertEquals(2, rebuilt.size)
    assertEquals("c", rebuilt[0].afterClipId)
    assertEquals(TransitionType.NONE, rebuilt[0].type)
    assertEquals("a", rebuilt[1].afterClipId)
    assertEquals(TransitionType.FADE_THROUGH_BLACK, rebuilt[1].type)
  }

  @Test
  fun audioSanitize_clampsVolumeAndTrim() {
    val bad =
      AudioTrackEdit(
          uri = "x",
          displayName = "x",
          sourceDurationMs = 3_000,
          trimStartMs = -10,
          trimEndMs = 9_999,
          volume = 5f,
          startOffsetMs = -20
        )
        .sanitized()
    assertTrue(bad.volume <= 2f)
    assertEquals(0L, bad.startOffsetMs)
    assertTrue(bad.trimStartMs >= 0L)
    assertTrue(bad.trimEndMs <= 3_000L)
    assertTrue(bad.trimEndMs > bad.trimStartMs)
  }

  @Test
  fun documentWithoutAudioTrack_isOptional() {
    val doc =
      EditDocument(
        clips = listOf(ClipEdit(uri = "v", displayName = "v", sourceDurationMs = 1000))
      )
    assertTrue(doc.audioTrack == null)
  }
}
