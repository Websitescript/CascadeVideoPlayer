package com.falah.cascade

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the player + media session. Gives the lock-screen / notification controls and the
 * ongoing media notification that Realme / ColorOS shows as a Live Alert while playing.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var exo: ExoPlayer? = null
    private var enhancer: LoudnessEnhancer? = null
    private var boostMb = 0
    private val boostCmd = SessionCommand("cascade.boost", Bundle.EMPTY)

    /** Volume above 100%: a clean gain stage on the audio session (up to +6 dB = 200%). */
    private fun applyBoost() {
        val id = exo?.audioSessionId ?: return
        try {
            if (enhancer == null) enhancer = LoudnessEnhancer(id)
            enhancer?.setTargetGain(boostMb)
            enhancer?.enabled = boostMb > 0
        } catch (_: Exception) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        // PREFER: use the FFmpeg extension decoders when they can handle a track
        val renderers = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        val player = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        exo = player
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                enhancer?.release(); enhancer = null
                if (boostMb > 0) applyBoost()
            }
        })
        session = MediaSession.Builder(this, player).setSessionActivity(open)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    val cmds = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(boostCmd).build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(cmds).build()
                }

                override fun onCustomCommand(
                    session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle,
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == "cascade.boost") {
                        boostMb = args.getInt("mb", 0).coerceIn(0, 700)
                        applyBoost()
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }).build()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this).apply { setSmallIcon(R.drawable.ic_notif) })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        enhancer?.release()
        enhancer = null
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
