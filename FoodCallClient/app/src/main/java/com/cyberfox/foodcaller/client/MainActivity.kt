package com.cyberfox.foodcaller.client

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private val clientUrl =
        "https://foodcall-tom.sirstilton.chatgpt.site/client"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This activity is also the full-screen view for a FoodCall alarm.
        if (AlarmStore.active(this)) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        webView = WebView(this)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false

            builtInZoomControls = false
            displayZoomControls = false

            setSupportZoom(false)

            loadWithOverviewMode = false
            useWideViewPort = true

            textZoom = 100
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(
                webView,
                true
            )
        }

        webView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                    // The bridge is deliberately available only to FoodCall pages.
                    return request.url.host != "foodcall-tom.sirstilton.chatgpt.site"
                }
            }

        webView.webChromeClient =
            WebChromeClient()

        webView.addJavascriptInterface(FoodCallBridge(), "FoodCallAndroid")

        setContentView(webView)

        if (savedInstanceState == null) {
            webView.loadUrl(clientUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }

        requestOverlayPermissionIfNeeded()
        if (android.os.Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            getSharedPreferences("foodcall", MODE_PRIVATE).edit().putString("fcm_token", token).apply()
            FcmRegistration.registerSaved(this, token)
        }
    }

    private inner class FoodCallBridge {
        @JavascriptInterface
        fun registerDevice(family: String, device: String, name: String) {
            FcmRegistration.rememberAndRegister(this@MainActivity, family, device, name)
        }

        @JavascriptInterface
        fun onFeedback(minutes: Int) = runOnUiThread {
            AlarmController.stop(this@MainActivity)
            if (minutes == 0) FoodAlertReceiver.showReminderSoon(this@MainActivity)
            else ReminderScheduler.schedule(this@MainActivity, minutes)
            finish()
        }

        @JavascriptInterface
        fun dismissAlarm() = runOnUiThread { AlarmController.stop(this@MainActivity); finish() }
    }

    private fun requestOverlayPermissionIfNeeded() {

        if (
            android.os.Build.VERSION.SDK_INT >= 23 &&
            !Settings.canDrawOverlays(this)
        ) {

            try {

                val intent =
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse(
                            "package:$packageName"
                        )
                    )

                startActivity(intent)

            } catch (_: Exception) {
            }
        }
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        webView.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
