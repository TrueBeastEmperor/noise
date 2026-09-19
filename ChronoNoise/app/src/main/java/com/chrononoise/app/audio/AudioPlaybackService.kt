package com.chrononoise.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.chrononoise.app.MainActivity
import com.chrononoise.app.R

class AudioPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "chrononoise_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.chrononoise.app.ACTION_STOP"
        const val ACTION_START = "com.chrononoise.app.ACTION_START"

        // Global sound engine instance accessible across activity & service
        val soundEngine = SoundEngine()
    }

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent focus loss (e.g. another music player started)
                resumeOnFocusGain = false
                soundEngine.stop()
                releaseWakeLock()
                updateNotification(isPlaying = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss (e.g. phone call ringing)
                if (soundEngine.isPlaying) {
                    resumeOnFocusGain = true
                    soundEngine.stop()
                    releaseWakeLock()
                    updateNotification(isPlaying = false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Transient loss with ducking permitted (e.g. navigation cue / notification chime)
                soundEngine.duckingMultiplier = 0.2f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Focus restored
                soundEngine.duckingMultiplier = 1.0f
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    acquireWakeLock()
                    soundEngine.start()
                    updateNotification(isPlaying = true)
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
        fun getEngine(): SoundEngine = soundEngine
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ChronoNoise::PlaybackWakeLock"
        ).apply {
            setReferenceCounted(false)
        }

        // Clean shutdown when sleep/focus timer finishes
        soundEngine.onTimerExpired = {
            releaseWakeLock()
            abandonAudioFocus()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                requestAudioFocus()
                startForeground(NOTIFICATION_ID, buildNotification(isPlaying = true))
                acquireWakeLock()
                soundEngine.start()
            }
            ACTION_STOP -> {
                soundEngine.stop()
                releaseWakeLock()
                abandonAudioFocus()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        soundEngine.stop()
        releaseWakeLock()
        abandonAudioFocus()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld != true) {
            // Safety timeout: 4 hours maximum (automatically released on stop/sleep timer)
            wakeLock?.acquire(4 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ChronoNoise audio playback notifications"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(isPlaying: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(isPlaying))
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPlaying) {
            "${soundEngine.bpm} BPM • ${soundEngine.clockType.displayName}"
        } else {
            "ChronoNoise Stopped"
        }

        val noiseInfo = if (soundEngine.noiseType != NoiseType.OFF) {
            " + ${soundEngine.noiseType.displayName}"
        } else {
            ""
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Playing spatial audio$noiseInfo")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.btn_stop), stopPendingIntent)
            .setOngoing(isPlaying)
            .setSilent(true)
            .build()
    }
}
