package com.cyberfox.foodcaller.client

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Sends the changing FCM token to FoodCall only after the website identifies this device. */
object FcmRegistration {
    private const val PREFS = "foodcall"
    private const val REGISTRATION_URL = "https://foodcall-tom.sirstilton.chatgpt.site/api/fcm-token"
    private val executor = Executors.newSingleThreadExecutor()

    fun rememberAndRegister(context: Context, family: String, device: String, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("family", family).putString("device", device).putString("name", name).apply()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> register(context, family, device, name, token) }
    }

    fun registerSaved(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val family = prefs.getString("family", "") ?: ""
        val device = prefs.getString("device", "") ?: ""
        val name = prefs.getString("name", "") ?: ""
        if (family.isNotBlank() && device.isNotBlank() && name.isNotBlank()) register(context, family, device, name, token)
    }

    private fun register(context: Context, family: String, device: String, name: String, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("fcm_token", token).apply()
        executor.execute {
            runCatching {
                val body = "{\"family\":${json(family)},\"device\":${json(device)},\"name\":${json(name)},\"token\":${json(token)}}"
                val connection = (URL(REGISTRATION_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 10_000
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                }
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                connection.inputStream.close(); connection.disconnect()
            }
        }
    }

    private fun json(value: String) = buildString {
        append('"'); value.forEach { c -> when (c) {
            '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> if (c.code < 32) append("\\u%04x".format(c.code)) else append(c)
        }}; append('"')
    }
}
