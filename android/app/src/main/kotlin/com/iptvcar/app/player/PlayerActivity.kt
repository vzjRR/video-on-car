package com.iptvcar.app.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.iptvcar.app.IptvCarApplication
import com.iptvcar.core.model.ContentType

/**
 * Native playback: Media3/ExoPlayer streams directly from the provider
 * URL. No file is ever fully downloaded before playback starts, and no
 * local proxy sits between the player and the provider (see
 * docs/ARCHITECTURE.md "Playback engine").
 */
class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PROVIDER_ID = "providerId"
        const val EXTRA_CONTENT_TYPE = "contentType"
        const val EXTRA_CONTENT_ID = "contentId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_STREAM_URL = "streamUrl"
    }

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val providerId = intent.getStringExtra(EXTRA_PROVIDER_ID)!!
        val contentType = ContentType.valueOf(intent.getStringExtra(EXTRA_CONTENT_TYPE)!!)
        val contentId = intent.getStringExtra(EXTRA_CONTENT_ID)!!
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)!!

        val app = application as IptvCarApplication
        viewModel.attach(app.resumeRepository, providerId, contentType, contentId, streamUrl)

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                PlayerScreen(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.persistResumePosition()
    }
}

@Composable
private fun PlayerScreen(viewModel: PlayerViewModel) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PlayerView(context).apply {
                player = viewModel.player
                useController = true
            }
        },
    )
}

class PlayerViewModel(application: android.app.Application) : AndroidViewModel(application) {

    val player: ExoPlayer by lazy { ExoPlayer.Builder(getApplication()).build() }

    private lateinit var resumeRepository: com.iptvcar.app.data.ResumeRepository
    private lateinit var providerId: String
    private lateinit var contentType: ContentType
    private lateinit var contentId: String
    private var attached = false

    fun attach(
        resumeRepository: com.iptvcar.app.data.ResumeRepository,
        providerId: String,
        contentType: ContentType,
        contentId: String,
        streamUrl: String,
    ) {
        this.resumeRepository = resumeRepository
        this.providerId = providerId
        this.contentType = contentType
        this.contentId = contentId

        if (!attached) {
            attached = true
            val resume = resumeRepository.get(providerId, contentType, contentId)
            player.setMediaItem(MediaItem.fromUri(streamUrl))
            player.prepare()
            if (resume != null && resume.positionMs > 0) {
                player.seekTo(resume.positionMs)
            }
            player.playWhenReady = true
        }
    }

    fun persistResumePosition() {
        if (!attached) return
        val duration = player.duration.coerceAtLeast(0)
        val position = player.currentPosition.coerceAtLeast(0)
        resumeRepository.save(
            com.iptvcar.core.model.ResumeState(
                providerId = providerId,
                contentType = contentType,
                contentId = contentId,
                positionMs = position,
                durationMs = duration,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    fun release() {
        player.release()
    }

    override fun onCleared() {
        release()
    }
}
