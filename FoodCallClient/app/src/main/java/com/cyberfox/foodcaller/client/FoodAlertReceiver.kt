package com.cyberfox.foodcaller.client
import android.content.*
import android.os.Handler
import android.os.Looper
import android.provider.Settings

class FoodAlertReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_READY = "com.cyberfox.foodcaller.client.FOOD_READY"
        const val ACTION_CLEAR = "com.cyberfox.foodcaller.client.FOOD_CLEAR"
        const val ACTION_REMINDER = "com.cyberfox.foodcaller.client.FOOD_REMINDER"
        const val ACTION_REMINDER_TIMEOUT = "com.cyberfox.foodcaller.client.FOOD_REMINDER_TIMEOUT"

        fun showReminder(context: Context) {
            AlarmStore.start(context, "reminder")
            ReminderScheduler.scheduleTimeout(context)
            if (Settings.canDrawOverlays(context)) runCatching {
                context.startForegroundService(Intent(context, FoodOverlayService::class.java).putExtra(FoodOverlayService.EXTRA_REMINDER, true))
            }
        }

        fun showReminderSoon(context: Context) {
            Handler(Looper.getMainLooper()).post { showReminder(context.applicationContext) }
        }
    }
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_READY -> {
                ReminderScheduler.cancel(context)
                AlarmStore.start(context, intent.getStringExtra("alarmId") ?: "adb")
                AlarmController.show(context, intent.getStringExtra("meal"))

                // Keep the native, clickable card on screen after the 3-second bell.
                if (Settings.canDrawOverlays(context)) {
                    runCatching {
                        context.startForegroundService(
                            Intent(context, FoodOverlayService::class.java)
                        )
                    }
                }
            }

            ACTION_CLEAR, ACTION_REMINDER_TIMEOUT -> AlarmController.stop(context)

            ACTION_REMINDER -> {
                showReminder(context)
            }

            "now", "5", "10", "later" -> {
                if (intent.action != "now") AlarmController.stop(context)
            }
        }
    }
}
