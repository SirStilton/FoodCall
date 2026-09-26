package com.cyberfox.foodcall

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class FoodNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()

        activeNotifications?.forEach { notification ->
            if (notification.packageName != packageName) {
                cancelNotification(notification.key)
            }
        }
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        if (sbn.packageName != packageName) {
            cancelNotification(sbn.key)
        }
    }
}
