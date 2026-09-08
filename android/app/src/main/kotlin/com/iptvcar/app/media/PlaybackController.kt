package com.iptvcar.app.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * Single shared ExoPlayer instance used by both the MediaLibraryService
 * (Android Auto/Automotive "Now Playing" surface) and the Android for Cars
 * App Library screens, so playback state is always consistent regardless
 * of which vehicle surface triggered it.
 */
object PlaybackController {

    @Volatile
    private var player: ExoPlayer? = null

    fun get(context: Context): ExoPlayer {
        return player ?: synchronized(this) {
            player ?: ExoPlayer.Builder(context.applicationContext).build().also { player = it }
        }
    }

    fun play(context: Context, streamUrl: String) {
        val exoPlayer = get(context)
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun release() {
        player?.release()
        player = null
    }
}
