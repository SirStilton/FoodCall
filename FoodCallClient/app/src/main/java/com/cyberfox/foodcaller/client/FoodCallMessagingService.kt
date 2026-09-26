package com.cyberfox.foodcaller.client

import android.content.Intent
import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives FCM data messages. The alarm remains native even if the website is down. */
class FoodCallMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        getSharedPreferences("foodcall", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
        FcmRegistration.registerSaved(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]?.uppercase()) {
            "FOOD_READY" -> {
                // A fresh meal call always replaces any pending or visible reminder.
                ReminderScheduler.cancel(this)
                AlarmController.stop(this)
                AlarmStore.start(this, message.data["alarmId"] ?: "")
                AlarmController.show(this, message.data["meal"])

                if (Settings.canDrawOverlays(this)) {
                    runCatching {
                        startForegroundService(
                            Intent(this, FoodOverlayService::class.java)
                        )
                    }
                }
            }

            "FOOD_CLEAR" -> AlarmController.stop(this)
        }
    }
}
