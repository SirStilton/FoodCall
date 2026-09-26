package com.cyberfox.foodcaller.client

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Keeps a requested FoodCall reminder alive after its WebView has been closed. */
object ReminderScheduler {
    private const val REMINDER_REQUEST = 4410
    private const val TIMEOUT_REQUEST = 4411

    fun schedule(context: Context, minutes: Int) {
        if (minutes <= 0) return
        // A previous reminder must never be able to close this newly requested one.
        alarm(context).cancel(pending(context, FoodAlertReceiver.ACTION_REMINDER_TIMEOUT, TIMEOUT_REQUEST))
        scheduleAt(context, FoodAlertReceiver.ACTION_REMINDER, REMINDER_REQUEST, System.currentTimeMillis() + minutes * 60_000L)
    }

    fun scheduleTimeout(context: Context) {
        alarm(context).cancel(pending(context, FoodAlertReceiver.ACTION_REMINDER_TIMEOUT, TIMEOUT_REQUEST))
        scheduleAt(context, FoodAlertReceiver.ACTION_REMINDER_TIMEOUT, TIMEOUT_REQUEST, System.currentTimeMillis() + 120_000L)
    }

    fun cancel(context: Context) {
        alarm(context).cancel(pending(context, FoodAlertReceiver.ACTION_REMINDER, REMINDER_REQUEST))
        alarm(context).cancel(pending(context, FoodAlertReceiver.ACTION_REMINDER_TIMEOUT, TIMEOUT_REQUEST))
    }

    private fun scheduleAt(context: Context, action: String, requestCode: Int, whenMillis: Long) {
        val manager = alarm(context)
        val operation = pending(context, action, requestCode)
        if (Build.VERSION.SDK_INT <= 30 || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, operation)
        } else {
            // Newer Android versions may require a separate exact-alarm permission.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, operation)
        }
    }

    private fun alarm(context: Context) = context.getSystemService(AlarmManager::class.java)
    private fun pending(context: Context, action: String, requestCode: Int) = PendingIntent.getBroadcast(
        context, requestCode, Intent(context, FoodAlertReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
