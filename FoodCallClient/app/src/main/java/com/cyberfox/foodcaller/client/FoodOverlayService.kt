package com.cyberfox.foodcaller.client

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * The website is the normal alarm UI and owns the response API.
 * Native UI is shown only while loading or when the website cannot be reached,
 * so a WebView failure can never leave a white empty overlay on screen.
 */
class FoodOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var root: FrameLayout? = null
    private var webView: WebView? = null
    private var fallback: View? = null
    private var loaded = false
    private var pageFailed = false
    private var reminder = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!AlarmStore.active(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            4402,
            NotificationCompat.Builder(this, AlarmController.CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("FoodCall-Alarm aktiv")
                .setOngoing(true)
                .build()
        )
        reminder = intent?.getBooleanExtra(EXTRA_REMINDER, false) == true
        if (root == null) showOverlay()
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val container = FrameLayout(this)
        val safeFallback = createFallback()
        val page = WebView(this).apply {
            visibility = View.INVISIBLE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    loaded = false
                    pageFailed = false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (url.startsWith("chrome-error://") || url.startsWith("data:text/html")) {
                        showFallback()
                        return
                    }
                    if (pageFailed) return
                    loaded = true
                    view.visibility = View.VISIBLE
                    safeFallback.visibility = View.GONE
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) = showFallback()

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: android.webkit.WebResourceError
                ) {
                    if (request.isForMainFrame) showFallback()
                }
            }
            addJavascriptInterface(OverlayBridge(), "FoodCallAndroid")
        }
        container.addView(page, FrameLayout.LayoutParams(-1, -1))
        // The fallback owns the whole overlay, so Android's own white error page
        // can never shine through behind it.
        container.addView(safeFallback, FrameLayout.LayoutParams(-1, -1, Gravity.TOP))

        root = container
        webView = page
        fallback = safeFallback
        windowManager.addView(
            container,
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE
            )
        )
        page.loadUrl(if (reminder) REMINDER_URL else CLIENT_URL)
        handler.postDelayed({ if (!loaded) showFallback() }, WEB_LOAD_TIMEOUT_MS)
    }

    private fun createFallback(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(36, 48, 36, 36)
        setBackgroundColor(Color.rgb(195, 40, 25))
        addView(TextView(context).apply {
            text = if (reminder) "ZEIT FÜRS ESSEN" else "ESSEN IST FERTIG"
            textSize = 26f
            setTextColor(Color.WHITE)
        })
        addView(TextView(context).apply {
            text = if (reminder) "Deine FoodCall-Erinnerung ist aktiv." else "Die FoodCall-Seite wird geladen …"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, 14, 0, 22)
        })
        addView(Button(context).apply {
            text = if (reminder) "unlock" else "Erneut laden"
            setOnClickListener {
                if (reminder) {
                    AlarmController.stop(this@FoodOverlayService)
                    return@setOnClickListener
                }
                pageFailed = false
                loaded = false
                webView?.visibility = View.INVISIBLE
                fallback?.visibility = View.VISIBLE
                webView?.reload()
                handler.postDelayed({ if (!loaded) showFallback() }, WEB_LOAD_TIMEOUT_MS)
            }
        })
    }

    private fun showFallback() {
        pageFailed = true
        // Android may call onPageFinished for its own error page first.
        // Always cover it: a white WebView error must never be the alarm UI.
        webView?.visibility = View.INVISIBLE
        fallback?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webView?.destroy()
        root?.let { runCatching { windowManager.removeView(it) } }
        webView = null
        fallback = null
        root = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class OverlayBridge {
        @JavascriptInterface
        fun onFeedback(minutes: Int) = handler.post {
            AlarmController.stop(this@FoodOverlayService)
            if (minutes == 0) FoodAlertReceiver.showReminderSoon(this@FoodOverlayService)
            else ReminderScheduler.schedule(this@FoodOverlayService, minutes)
        }

        @JavascriptInterface
        fun dismissAlarm() = handler.post { AlarmController.stop(this@FoodOverlayService) }
    }

    companion object {
        const val EXTRA_REMINDER = "foodcall_reminder"
        const val CLIENT_URL = "https://foodcall-tom.sirstilton.chatgpt.site/client?nativeAlarm=1"
        const val REMINDER_URL = "https://foodcall-tom.sirstilton.chatgpt.site/client?nativeReminder=1"
        const val WEB_LOAD_TIMEOUT_MS = 3_000L
    }
}
