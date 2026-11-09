package com.capcutmini.app.media.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Configurable range: 0.5s–3s (stored as ms). 0 means disabled. */
object FadeLimits {
    const val MIN_MS = 500L
    const val MAX_MS = 3_000L

    fun sanitize(requestedMs: Long): Long {
        if (requestedMs <= 0L) return 0L
        return requestedMs.coerceIn(MIN_MS, MAX_MS)
    }

    fun clampPair(fadeInMs: Long, fadeOutMs: Long, clipDurationMs: Long): Pair<Long, Long> {
        val safeIn = sanitize(fadeInMs)
        val safeOut = sanitize(fadeOutMs)
        if (clipDurationMs <= 0L) return 0L to 0L
        if (safeIn + safeOut <= clipDurationMs) return safeIn to safeOut
        if (clipDurationMs < MIN_MS) return 0L to 0L
        val half = clipDurationMs / 2
        return minOf(safeIn, half) to minOf(safeOut, clipDurationMs - minOf(safeIn, half))
    }
}

enum class TransitionType {
    NONE,
    FADE_THROUGH_BLACK,
    CROSS_DISSOLVE_FALLBACK
}

data class ClipEdit(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val displayName: String,
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = sourceDurationMs,
    val videoFadeInMs: Long = 0L,
    val videoFadeOutMs: Long = 0L,
    val audioFadeInMs: Long = 0L,
    val audioFadeOutMs: Long = 0L
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    fun withClampedFades(): ClipEdit {
        val (vIn, vOut) = FadeLimits.clampPair(videoFadeInMs, videoFadeOutMs, trimmedDurationMs)
        val (aIn, aOut) = FadeLimits.clampPair(audioFadeInMs, audioFadeOutMs, trimmedDurationMs)
        return copy(
            videoFadeInMs = vIn,
            videoFadeOutMs = vOut,
            audioFadeInMs = aIn,
            audioFadeOutMs = aOut,
            trimStartMs = trimStartMs.coerceIn(0L, sourceDurationMs),
            trimEndMs = trimEndMs.coerceIn(trimStartMs, sourceDurationMs).let {
                if (it <= trimStartMs) sourceDurationMs else it
            }
        )
    }
}

/**
 * Secondary audio track mixed under the video timeline in Transformer export.
 * [startOffsetMs] delays the audio on the project timeline.
 */
data class AudioTrackEdit(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val displayName: String,
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = sourceDurationMs,
    val volume: Float = 1f,
    val startOffsetMs: Long = 0L
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    fun sanitized(): AudioTrackEdit {
        val start = trimStartMs.coerceIn(0L, sourceDurationMs)
        val end = trimEndMs.coerceIn(start + 50L, sourceDurationMs).let {
            if (it <= start) sourceDurationMs else it
        }
        return copy(
            trimStartMs = start,
            trimEndMs = end,
            volume = volume.coerceIn(0f, 2f),
            startOffsetMs = startOffsetMs.coerceAtLeast(0L)
        )
    }
}

data class SubtitleCue(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class TransitionEdit(
    val afterClipId: String,
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = FadeLimits.MIN_MS
)

data class EditDocument(
    val projectTitle: String = "Untitled",
    val clips: List<ClipEdit> = emptyList(),
    val transitions: List<TransitionEdit> = emptyList(),
    val subtitles: List<SubtitleCue> = emptyList(),
    val audioTrack: AudioTrackEdit? = null
) {
    fun totalTimelineMs(): Long = clips.sumOf { it.trimmedDurationMs }

    fun toJson(): String {
        val root = JSONObject()
        root.put("projectTitle", projectTitle)
        root.put("clips", JSONArray().also { arr ->
            clips.forEach { c ->
                arr.put(
                    JSONObject()
                        .put("id", c.id)
                        .put("uri", c.uri)
                        .put("displayName", c.displayName)
                        .put("sourceDurationMs", c.sourceDurationMs)
                        .put("trimStartMs", c.trimStartMs)
                        .put("trimEndMs", c.trimEndMs)
                        .put("videoFadeInMs", c.videoFadeInMs)
                        .put("videoFadeOutMs", c.videoFadeOutMs)
                        .put("audioFadeInMs", c.audioFadeInMs)
                        .put("audioFadeOutMs", c.audioFadeOutMs)
                )
            }
        })
        root.put("transitions", JSONArray().also { arr ->
            transitions.forEach { t ->
                arr.put(
                    JSONObject()
                        .put("afterClipId", t.afterClipId)
                        .put("type", t.type.name)
                        .put("durationMs", t.durationMs)
                )
            }
        })
        root.put("subtitles", JSONArray().also { arr ->
            subtitles.forEach { s ->
                arr.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("text", s.text)
                        .put("startMs", s.startMs)
                        .put("endMs", s.endMs)
                )
            }
        })
        audioTrack?.let { a ->
            root.put(
                "audioTrack",
                JSONObject()
                    .put("id", a.id)
                    .put("uri", a.uri)
                    .put("displayName", a.displayName)
                    .put("sourceDurationMs", a.sourceDurationMs)
                    .put("trimStartMs", a.trimStartMs)
                    .put("trimEndMs", a.trimEndMs)
                    .put("volume", a.volume.toDouble())
                    .put("startOffsetMs", a.startOffsetMs)
            )
        }
        return root.toString()
    }

    companion object {
        fun fromJson(json: String?): EditDocument {
            if (json.isNullOrBlank()) return EditDocument()
            return runCatching {
                val root = JSONObject(json)
                val clips = buildList {
                    val arr = root.optJSONArray("clips") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            ClipEdit(
                                id = o.getString("id"),
                                uri = o.getString("uri"),
                                displayName = o.optString("displayName", "Clip"),
                                sourceDurationMs = o.optLong("sourceDurationMs"),
                                trimStartMs = o.optLong("trimStartMs"),
                                trimEndMs = o.optLong("trimEndMs"),
                                videoFadeInMs = o.optLong("videoFadeInMs"),
                                videoFadeOutMs = o.optLong("videoFadeOutMs"),
                                audioFadeInMs = o.optLong("audioFadeInMs"),
                                audioFadeOutMs = o.optLong("audioFadeOutMs")
                            ).withClampedFades()
                        )
                    }
                }
                val transitions = buildList {
                    val arr = root.optJSONArray("transitions") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            TransitionEdit(
                                afterClipId = o.getString("afterClipId"),
                                type = runCatching {
                                    TransitionType.valueOf(o.getString("type"))
                                }.getOrDefault(TransitionType.NONE),
                                durationMs = FadeLimits.sanitize(o.optLong("durationMs", FadeLimits.MIN_MS))
                                    .let { if (it == 0L) FadeLimits.MIN_MS else it }
                            )
                        )
                    }
                }
                val subtitles = buildList {
                    val arr = root.optJSONArray("subtitles") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            SubtitleCue(
                                id = o.getString("id"),
                                text = o.optString("text"),
                                startMs = o.optLong("startMs"),
                                endMs = o.optLong("endMs")
                            )
                        )
                    }
                }
                val audio = root.optJSONObject("audioTrack")?.let { o ->
                    AudioTrackEdit(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        uri = o.getString("uri"),
                        displayName = o.optString("displayName", "Audio"),
                        sourceDurationMs = o.optLong("sourceDurationMs"),
                        trimStartMs = o.optLong("trimStartMs"),
                        trimEndMs = o.optLong("trimEndMs"),
                        volume = o.optDouble("volume", 1.0).toFloat(),
                        startOffsetMs = o.optLong("startOffsetMs")
                    ).sanitized()
                }
                EditDocument(
                    projectTitle = root.optString("projectTitle", "Untitled"),
                    clips = clips,
                    transitions = transitions,
                    subtitles = subtitles,
                    audioTrack = audio
                )
            }.getOrDefault(EditDocument())
        }
    }
}

data class ResolvedClipEffects(
    val clip: ClipEdit,
    val videoFadeInMs: Long,
    val videoFadeOutMs: Long,
    val audioFadeInMs: Long,
    val audioFadeOutMs: Long
)

fun EditDocument.resolveClipEffects(): List<ResolvedClipEffects> {
    if (clips.isEmpty()) return emptyList()
    val transitionByAfterId = transitions.associateBy { it.afterClipId }
    return clips.mapIndexed { index, raw ->
        val clip = raw.withClampedFades()
        var vIn = clip.videoFadeInMs
        var vOut = clip.videoFadeOutMs
        var aIn = clip.audioFadeInMs
        var aOut = clip.audioFadeOutMs

        if (index > 0) {
            val prev = clips[index - 1]
            val t = transitionByAfterId[prev.id]
            if (t != null && t.type != TransitionType.NONE) {
                val d = FadeLimits.sanitize(t.durationMs).coerceAtLeast(FadeLimits.MIN_MS)
                vIn = maxOf(vIn, d)
                aIn = maxOf(aIn, d)
            }
        }
        val outT = transitionByAfterId[clip.id]
        if (outT != null && outT.type != TransitionType.NONE && index < clips.lastIndex) {
            val d = FadeLimits.sanitize(outT.durationMs).coerceAtLeast(FadeLimits.MIN_MS)
            vOut = maxOf(vOut, d)
            aOut = maxOf(aOut, d)
        }

        val (cvIn, cvOut) = FadeLimits.clampPair(vIn, vOut, clip.trimmedDurationMs)
        val (caIn, caOut) = FadeLimits.clampPair(aIn, aOut, clip.trimmedDurationMs)
        ResolvedClipEffects(clip, cvIn, cvOut, caIn, caOut)
    }
}

/** Rebuild transition list after clip reorder/remove while preserving known settings. */
fun rebuildTransitions(
    clips: List<ClipEdit>,
    existing: List<TransitionEdit>
): List<TransitionEdit> {
    val byId = existing.associateBy { it.afterClipId }
    return clips.zipWithNext().map { (a, _) ->
        byId[a.id] ?: TransitionEdit(afterClipId = a.id, type = TransitionType.NONE)
    }
}
