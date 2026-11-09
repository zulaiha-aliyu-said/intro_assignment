# Milestone: Video/Audio Fades & Transitions

## Implemented (real Media3 pipeline)

| Feature | Preview | Export (Transformer) |
|---|---|---|
| Video fade in/out (0.5–3s, clamped on short clips) | Black overlay driven by playback envelope | `VideoFadeRgbMatrix` (`RgbMatrix`) |
| Audio fade in/out | `ExoPlayer.setVolume` envelope (A/V synced) | `AudioFadeProcessor` (`BaseAudioProcessor`) |
| Fade-through-black transition | Same envelopes on adjacent clips | Same effects on consecutive `EditedMediaItem`s |
| Manual subtitles | Compose overlay | `SubtitleBurnOverlay` (`BitmapOverlay`) |
| Trim | ExoPlayer clipping | `MediaItem.ClippingConfiguration` |
| Persist settings | Room `editJson` on project | — |

## Limitation / alternative

**Native overlapping cross-dissolve** is not supported by Media3 **1.5.1** on a single `EditedMediaItemSequence` (items are concatenated, not overlapped). The UI option **Cross\*** maps to the tested **fade-through-black** alternative and is labeled in the Effects panel.

## Not in this milestone

- AI video generation
- UI-only fake transitions (none were added)
