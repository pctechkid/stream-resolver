package com.ronnel.streamresolver

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {

    private val mpvKtPackage = "live.mehiz.mpvkt"

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupOverlayWindow()
        showModernLoadingOverlay()

        val originalUrl = getIncomingUrl(intent)

        if (originalUrl.isNullOrBlank()) {
            finish()
            return
        }

        Thread {
            val finalUrl = try {
                if (isHttpUrl(originalUrl)) {
                    resolveFinalUrl(originalUrl)
                } else {
                    originalUrl
                }
            } catch (e: Exception) {
                originalUrl
            }

            runOnUiThread {
                openInMpvKt(finalUrl)
            }
        }.start()
    }

    private fun setupOverlayWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setBackgroundDrawableResource(android.R.color.transparent)

        val params = window.attributes
        params.dimAmount = 0.45f
        window.attributes = params

        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun showModernLoadingOverlay() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val loadingCard = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22f)
                setColor(Color.parseColor("#CC111111"))
            }

            elevation = dp(10f)
            setPadding(dp(18f).toInt(), dp(18f).toInt(), dp(18f).toInt(), dp(18f).toInt())
        }

        val spinner = ModernSpinnerView(this)

        val spinnerParams = FrameLayout.LayoutParams(
            dp(46f).toInt(),
            dp(46f).toInt()
        ).apply {
            gravity = Gravity.CENTER
        }

        loadingCard.addView(spinner, spinnerParams)

        val cardParams = FrameLayout.LayoutParams(
            dp(86f).toInt(),
            dp(86f).toInt()
        ).apply {
            gravity = Gravity.CENTER
        }

        root.addView(loadingCard, cardParams)
        setContentView(root)
    }

    private fun getIncomingUrl(intent: Intent?): String? {
        if (intent == null) return null

        intent.dataString?.let {
            return cleanUrl(it)
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

        if (!sharedText.isNullOrBlank()) {
            val regex = Regex("""https?://[^\s]+""")
            return regex.find(sharedText)?.value?.let { cleanUrl(it) }
        }

        return null
    }

    private fun cleanUrl(url: String): String {
        return url.trim()
            .removeSuffix("\"")
            .removeSuffix("'")
            .removeSuffix(")")
            .removeSuffix("]")
    }

    private fun isHttpUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
    }

    private fun resolveFinalUrl(url: String): String {
        val headRequest = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "Mozilla/5.0")
            .build()

        try {
            client.newCall(headRequest).execute().use { response ->
                val finalUrl = response.request.url.toString()

                if (finalUrl != url) {
                    return finalUrl
                }
            }
        } catch (_: Exception) {
            // Some servers reject HEAD. Fallback to ranged GET.
        }

        val getRequest = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-0")
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(getRequest).execute().use { response ->
            return response.request.url.toString()
        }
    }

    private fun openInMpvKt(url: String) {
        val uri = Uri.parse(url)

        val mpvIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            setPackage(mpvKtPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(mpvIntent)
        } catch (e: ActivityNotFoundException) {
            openWithChooser(uri)
        } catch (e: Exception) {
            openWithChooser(uri)
        }

        finish()
    }

    private fun openWithChooser(uri: Uri) {
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(Intent.createChooser(fallbackIntent, "Open final stream with"))
        } catch (_: Exception) {
            finish()
        }
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}

class ModernSpinnerView(context: Context) : View(context) {

    private var rotationAngle = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        strokeCap = Paint.Cap.ROUND
    }

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 850L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val strokePadding = dp(6f)
        val rect = RectF(
            strokePadding,
            strokePadding,
            width - strokePadding,
            height - strokePadding
        )

        canvas.drawArc(rect, 0f, 360f, false, backgroundPaint)
        canvas.drawArc(rect, rotationAngle, 115f, false, paint)
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}