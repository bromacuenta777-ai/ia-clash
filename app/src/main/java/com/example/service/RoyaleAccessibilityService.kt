package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.example.model.ArenaCalibration
import com.example.model.BotRuntimeBridge
import com.example.model.GestureMode
import com.example.model.MoveSource
import com.example.model.TacticalMove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "RoyaleAccessibility"

class RoyaleAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    private var statusBadgeView: TextView? = null
    private var elixirLabelView: TextView? = null
    private var moveLabelView: TextView? = null
    private var togglePlayButton: TextView? = null

    companion object {
        @Volatile
        var instance: RoyaleAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        BotRuntimeBridge.setAccessibilityConnected(true)
        BotRuntimeBridge.appendLog(
            tag = "ACCESIBILIDAD",
            message = "Servicio táctil conectado. Gestos autónomos listos sin PC.",
            isHighlight = true
        )

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        observeRuntimeForOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank()) {
                BotRuntimeBridge.updateForegroundPackage(pkg)
            }
        }
    }

    override fun onInterrupt() {
        BotRuntimeBridge.appendLog("ACCESIBILIDAD", "Servicio interrumpido por el sistema.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        BotRuntimeBridge.setAccessibilityConnected(false)
        BotRuntimeBridge.setBotPlaying(false)
        removeOverlay()
        serviceScope.cancel()
    }

    private fun observeRuntimeForOverlay() {
        serviceScope.launch {
            combine(
                BotRuntimeBridge.overlayVisible,
                BotRuntimeBridge.captureRunning,
                BotRuntimeBridge.botPlaying,
                BotRuntimeBridge.visionState,
                BotRuntimeBridge.lastMove
            ) { overlayVisible, captureRunning, botPlaying, vision, lastMove ->
                OverlaySnapshot(overlayVisible, captureRunning, botPlaying, vision.elixir, lastMove)
            }.collect { snap ->
                if (snap.overlayVisible && snap.captureRunning) {
                    ensureOverlayAttached()
                    updateOverlayContent(snap)
                } else {
                    removeOverlay()
                }
            }
        }
    }

    private fun ensureOverlayAttached() {
        if (overlayView != null) return
        val wm = windowManager ?: return

        val density = resources.displayMetrics.density
        val padH = (12 * density).toInt()
        val padV = (10 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E60B1120"))
                cornerRadius = 16 * density
                setStroke((1.5f * density).toInt(), Color.parseColor("#00E5FF"))
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = "ROYALE AI"
            textSize = 11f
            setTextColor(Color.parseColor("#F5B041"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val statusView = TextView(this).apply {
            text = "  • EN ESPERA"
            textSize = 11f
            setTextColor(Color.parseColor("#9FB0D1"))
        }
        statusBadgeView = statusView

        headerRow.addView(titleView)
        headerRow.addView(statusView)

        val elixirView = TextView(this).apply {
            text = "Elixir: 0.0  |  Sin jugada"
            textSize = 11f
            setTextColor(Color.parseColor("#E040FB"))
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
        }
        elixirLabelView = elixirView

        val moveView = TextView(this).apply {
            text = "Listo para entrar a Clash Royale"
            textSize = 10f
            setTextColor(Color.parseColor("#F1F5FF"))
            maxLines = 2
        }
        moveLabelView = moveView

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (8 * density).toInt(), 0, 0)
        }

        val playBtn = TextView(this).apply {
            text = "INICIAR IA"
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#070B14"))
            setPadding((10 * density).toInt(), (7 * density).toInt(), (10 * density).toInt(), (7 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#00E676"))
                cornerRadius = 10 * density
            }
            setOnClickListener {
                BotRuntimeBridge.toggleBotPlaying()
            }
        }
        togglePlayButton = playBtn

        val geminiBtn = TextView(this).apply {
            text = "GEMINI"
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#00E5FF"))
            setPadding((10 * density).toInt(), (7 * density).toInt(), (10 * density).toInt(), (7 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0A3342"))
                cornerRadius = 10 * density
                setStroke((1 * density).toInt(), Color.parseColor("#00E5FF"))
            }
            setOnClickListener {
                BotRuntimeBridge.onRequestInstantGeminiScan?.invoke()
            }
        }

        val hideBtn = TextView(this).apply {
            text = "✕"
            textSize = 12f
            setTextColor(Color.parseColor("#FF5252"))
            setPadding((10 * density).toInt(), (7 * density).toInt(), (8 * density).toInt(), (7 * density).toInt())
            setOnClickListener {
                BotRuntimeBridge.setOverlayVisible(false)
            }
        }

        val spacer1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1)
        }
        val spacer2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1)
        }

        controlsRow.addView(playBtn)
        controlsRow.addView(spacer1)
        controlsRow.addView(geminiBtn)
        controlsRow.addView(spacer2)
        controlsRow.addView(hideBtn)

        root.addView(headerRow)
        root.addView(elixirView)
        root.addView(moveView)
        root.addView(controlsRow)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (14 * density).toInt()
            y = (76 * density).toInt()
        }

        attachDragBehavior(root, params, wm)

        try {
            wm.addView(root, params)
            overlayView = root
            overlayParams = params
        } catch (e: Exception) {
            Log.e(TAG, "Unable to attach accessibility overlay", e)
        }
    }

    private fun attachDragBehavior(
        view: View,
        params: WindowManager.LayoutParams,
        wm: WindowManager
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        moved = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            wm.updateViewLayout(view, params)
                        } catch (_: Exception) {
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> moved
            }
        }
    }

    private fun updateOverlayContent(snap: OverlaySnapshot) {
        statusBadgeView?.apply {
            text = if (snap.botPlaying) "  • JUGANDO IA" else "  • PAUSADA"
            setTextColor(
                if (snap.botPlaying) Color.parseColor("#00E676") else Color.parseColor("#F5B041")
            )
        }
        elixirLabelView?.text = "Elixir: ${"%.1f".format(snap.elixir)}"
        moveLabelView?.text = snap.lastMove?.let {
            "${it.cardName} -> ${it.tileLabel}"
        } ?: "Esperando ventana táctica..."

        togglePlayButton?.apply {
            text = if (snap.botPlaying) "PAUSAR" else "INICIAR IA"
            background = GradientDrawable().apply {
                setColor(
                    if (snap.botPlaying) Color.parseColor("#F5B041") else Color.parseColor("#00E676")
                )
                cornerRadius = 10 * resources.displayMetrics.density
            }
        }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }
        overlayView = null
        overlayParams = null
    }

    fun getRealScreenSize(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        val w = if (metrics.widthPixels > 0) metrics.widthPixels else resources.displayMetrics.widthPixels
        val h = if (metrics.heightPixels > 0) metrics.heightPixels else resources.displayMetrics.heightPixels
        return w to h
    }

    fun dispatchTap(x: Float, y: Float, durationMs: Long = 55L, onDone: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + 1f, y + 1f)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(30L, 250L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    onDone?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    onDone?.invoke(false)
                }
            },
            mainHandler
        )
    }

    fun executeTacticalMove(
        move: TacticalMove,
        cardPoint: PointF?,
        targetPoint: PointF,
        gestureMode: GestureMode,
        calibration: ArenaCalibration
    ) {
        if (move.source == MoveSource.AUTO_QUEUE || cardPoint == null) {
            dispatchTap(targetPoint.x, targetPoint.y)
            return
        }

        if (gestureMode == GestureMode.DRAG_AND_DROP) {
            val path = Path().apply {
                moveTo(cardPoint.x, cardPoint.y)
                lineTo(targetPoint.x, targetPoint.y)
            }
            val duration = calibration.gestureSpeedMs.coerceIn(70L, 350L)
            val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, mainHandler)
        } else {
            // Tap card slot, then tap arena tile after 70ms
            dispatchTap(cardPoint.x, cardPoint.y, durationMs = 45L) { ok ->
                if (ok) {
                    mainHandler.postDelayed({
                        dispatchTap(targetPoint.x, targetPoint.y, durationMs = 45L)
                    }, 70L)
                }
            }
        }
    }

    private data class OverlaySnapshot(
        val overlayVisible: Boolean,
        val captureRunning: Boolean,
        val botPlaying: Boolean,
        val elixir: Float,
        val lastMove: TacticalMove?
    )
}
