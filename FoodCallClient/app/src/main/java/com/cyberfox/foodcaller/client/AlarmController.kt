package com.cyberfox.foodcaller.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AlarmController {
    const val CHANNEL = "foodcall_alarm"
    private const val NOTIFICATION_ID = 4401
    private var alarmPlayer: MediaPlayer? = null

    private fun actionIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, action.hashCode(),
            Intent(context, FoodAlertReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun show(context: Context, meal: String? = null) {
        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(CHANNEL, "Essensalarm", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 700, 350, 700)
                    setSound(null, alarmAttributes) // Custom in-app bell plays below.
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )

        val fullScreen = PendingIntent.getActivity(
            context, 0,
            // The normal alarm surface is the existing client website, not a second UI.
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("ESSEN IST FERTIG")
            .setContentText("Bitte gib kurz Bescheid.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            // The bell stops after three seconds, but this actionable alert stays
            // until a response is selected or FOOD_CLEAR arrives.
            .setOnlyAlertOnce(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, mealIcon(meal)))
            .addAction(0, "Jetzt", actionIntent(context, "now"))
            .addAction(0, "5 min", actionIntent(context, "5"))
            .addAction(0, "10 min", actionIntent(context, "10"))
            .addAction(0, "Später", actionIntent(context, "later"))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        startAlarmStream(context, alarmAttributes)
        // One short attention signal, not a vibration loop while someone decides.
        // The silent, clickable overlay remains until a response or FOOD_CLEAR.
        context.getSystemService(Vibrator::class.java).vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 500, 200, 500, 200, 500, 200, 500, 400),
                -1
            ),
            alarmAttributes
        )
    }

    private fun mealIcon(meal: String?) = when (meal?.uppercase()) {
        "BREAKFAST" -> R.drawable.ic_meal_breakfast
        "LUNCH" -> R.drawable.ic_meal_lunch
        "DINNER" -> R.drawable.ic_meal_dinner
        else -> R.drawable.ic_meal_snack
    }

    /** Explicitly uses Android's ALARM stream, which is separate from media/silent mode. */
    private fun startAlarmStream(context: Context, attributes: AudioAttributes) {
        if (alarmPlayer?.isPlaying == true) return
        runCatching {
            val descriptor = context.resources.openRawResourceFd(R.raw.opening_bell)
                ?: return
            val player = MediaPlayer().apply {
                // Samsung A40 otherwise routes MediaPlayer through muted STREAM_MUSIC.
                // STREAM_ALARM is separate from ringer/media silent mode.
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_ALARM)
                setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
                descriptor.close()
                prepare()
                setVolume(0.6f, 0.6f)
                start()
            }
            alarmPlayer = player
            Handler(Looper.getMainLooper()).postDelayed({
                if (alarmPlayer === player) {
                    player.stop()
                    player.release()
                    alarmPlayer = null
                }
            }, 3_000)
        }
    }

    fun stop(context: Context) {
        alarmPlayer?.run { stop(); release() }
        alarmPlayer = null
        context.getSystemService(Vibrator::class.java).cancel()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        AlarmStore.clear(context)
        context.stopService(Intent(context, FoodOverlayService::class.java))
    }
}
