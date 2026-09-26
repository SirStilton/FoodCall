package com.cyberfox.foodcall

import android.app.Activity
import android.app.NotificationManager
import android.app.WallpaperManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private val foodCallUrl =
        "https://foodcall-tom.sirstilton.chatgpt.site/caller"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        hideSystemUI()
        setFoodCallWallpaper()

        webView = WebView(this)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            loadWithOverviewMode = false
            useWideViewPort = true
            textZoom = 100
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                val js = """
                    (function() {

                        var viewport =
                            document.querySelector('meta[name="viewport"]');

                        if (!viewport) {
                            viewport = document.createElement('meta');
                            viewport.name = 'viewport';
                            document.head.appendChild(viewport);
                        }

                        viewport.content =
                            'width=device-width,' +
                            'initial-scale=1.0,' +
                            'maximum-scale=1.0,' +
                            'user-scalable=no';

                        var style =
                            document.getElementById('foodcall-kiosk-fix');

                        if (!style) {
                            style = document.createElement('style');
                            style.id = 'foodcall-kiosk-fix';

                            style.innerHTML = `
                                html,
                                body {
                                    width: 100% !important;
                                    height: 100% !important;
                                    min-height: 100% !important;
                                    margin: 0 !important;
                                    padding: 0 !important;
                                    overflow: hidden !important;
                                }

                                #root,
                                #app,
                                main {
                                    min-height: 100vh !important;
                                }
                            `;

                            document.head.appendChild(style);
                        }

                    })();
                """.trimIndent()

                view?.evaluateJavascript(
                    js,
                    null
                )
            }
        }

        webView.webChromeClient = WebChromeClient()

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        setContentView(webView)

        if (savedInstanceState == null) {
            webView.loadUrl(foodCallUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }

        configureKioskMode()
    }

    private fun configureKioskMode() {
        val dpm =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        val admin =
            ComponentName(this, AdminReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(packageName)) {
            return
        }

        try {
            dpm.setLockTaskPackages(
                admin,
                arrayOf(packageName)
            )

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                dpm.setLockTaskFeatures(
                    admin,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                )
            }
        } catch (_: Exception) {
        }

        try {
            dpm.setKeyguardDisabled(
                admin,
                true
            )
        } catch (_: Exception) {
        }

        try {
            dpm.setStatusBarDisabled(
                admin,
                true
            )
        } catch (_: Exception) {
        }

        try {
            val filter =
                IntentFilter(Intent.ACTION_MAIN)

            filter.addCategory(
                Intent.CATEGORY_HOME
            )

            filter.addCategory(
                Intent.CATEGORY_DEFAULT
            )

            dpm.addPersistentPreferredActivity(
                admin,
                filter,
                ComponentName(
                    packageName,
                    MainActivity::class.java.name
                )
            )
        } catch (_: Exception) {
        }

        try {
            startLockTask()
        } catch (_: Exception) {
        }

        silenceNotifications()
    }

    private fun silenceNotifications() {
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.isNotificationPolicyAccessGranted) {
            try {
                manager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_NONE
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun setFoodCallWallpaper() {
        val prefs =
            getSharedPreferences(
                "foodcall_wallpaper",
                MODE_PRIVATE
            )

        if (prefs.getBoolean("set", false)) {
            return
        }

        try {
            val bitmap =
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.foodcall_wallpaper
                )

            val manager =
                WallpaperManager.getInstance(this)

            manager.setBitmap(bitmap)

            prefs.edit()
                .putBoolean("set", true)
                .apply()

        } catch (_: Exception) {
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onResume() {
        super.onResume()

        hideSystemUI()

        val dpm =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (dpm.isDeviceOwnerApp(packageName)) {
            try {
                startLockTask()
            } catch (_: Exception) {
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }
}
