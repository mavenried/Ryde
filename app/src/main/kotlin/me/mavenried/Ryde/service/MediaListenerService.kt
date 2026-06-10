package me.mavenried.Ryde.service

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val albumArt: Bitmap? = null
)

class MediaListenerService : NotificationListenerService() {

    companion object {
        private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
        val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

        private var transportControls: MediaController.TransportControls? = null

        fun play() { transportControls?.play() }
        fun pause() { transportControls?.pause() }
        fun skipToNext() { transportControls?.skipToNext() }
        fun skipToPrevious() { transportControls?.skipToPrevious() }
    }

    private var activeController: MediaController? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refreshNowPlaying()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshNowPlaying()
    }

    override fun onListenerConnected() = refreshActiveSessions()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refreshActiveSessions()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refreshActiveSessions()

    private fun refreshActiveSessions() {
        val mgr = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sessions = try {
            mgr.getActiveSessions(ComponentName(this, MediaListenerService::class.java))
        } catch (_: Exception) {
            emptyList()
        }
        activeController?.unregisterCallback(mediaCallback)
        activeController = sessions.firstOrNull()
        activeController?.registerCallback(mediaCallback)
        transportControls = activeController?.transportControls
        refreshNowPlaying()
    }

    private fun refreshNowPlaying() {
        val ctrl = activeController ?: run { _nowPlaying.value = null; return }
        val meta = ctrl.metadata
        _nowPlaying.value = NowPlaying(
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            isPlaying = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING,
            albumArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        )
    }
}
