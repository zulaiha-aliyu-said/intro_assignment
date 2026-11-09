package com.capcutmini.app.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Thin Media3 ExoPlayer wrapper for local video preview.
 * No editing or export logic lives here yet.
 */
class VideoPlayerController(
    context: Context
) {
    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = false
    }

    fun setVideoUri(uri: Uri?) {
        if (uri == null) {
            player.stop()
            player.clearMediaItems()
            return
        }
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun release() {
        player.release()
    }
}
