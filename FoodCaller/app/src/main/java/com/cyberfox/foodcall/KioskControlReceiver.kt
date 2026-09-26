package com.cyberfox.foodcall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class KioskControlReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val prefs =
            context.getSharedPreferences(
                "foodcall_kiosk",
                Context.MODE_PRIVATE
            )

        when (intent.action) {

            "com.cyberfox.foodcall.ENABLE_KIOSK" -> {

                prefs.edit()
                    .putBoolean("enabled", true)
                    .apply()

                val start =
                    Intent(
                        context,
                        MainActivity::class.java
                    )

                start.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )

                context.startActivity(start)
            }

            "com.cyberfox.foodcall.DISABLE_KIOSK" -> {

                prefs.edit()
                    .putBoolean("enabled", false)
                    .apply()
            }
        }
    }
}
