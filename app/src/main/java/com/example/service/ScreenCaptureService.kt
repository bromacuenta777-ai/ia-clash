package com.example.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.PointF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.ArenaVisionAnalyzer
import com.example.engine.GeminiVisionStrategist
import com.example.engine.Top1TacticalBrain
import com.example.model.BattlePhase
import com.example.model.BotRuntimeBridge
import com.example.model.MoveSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "ScreenCaptureService"
private const val CHANNEL_ID = "royale_ai_vision_channel"
private const val NOTIFICATION_ID = 7001

class ScreenCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val ACTION_STOP = "com.example.action.STOP_CAPTURE"

        fun createStartIntent(context: Context, resultCode: Int, data: Intent): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null
    private var geminiJob: Job? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val visionAnalyzer = ArenaVisionAnalyzer()
    private val tacticalBrain = Top1TacticalBrain()
    private val geminiStrategist = GeminiVisionStrategist()

    private var lastGeminiScanMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        BotRuntimeBridge.onRequestInstantGeminiScan = {
            triggerGeminiScan(forceNow = true)
        }
        BotRuntimeBridge.onRequestStopCaptureService = {
            stopCaptureAndSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCaptureAndSelf()
            return START_NOT_STICKY
        }

        // Obligatorio en Android 14+ llamar startForeground ANTES de getMediaProjection
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED

        @Suppress("DEPRECATION")
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            BotRuntimeBridge.appendLog("VISIÓN", "Permiso de captura de pantalla no recibido.")
            stopCaptureAndSelf()
            return START_NOT_STICKY
        }

        startProjectionPipeline(resultCode, data)
        return START_NOT_STICKY
    }

    @SuppressLint("WrongConstant")
    private fun startProjectionPipeline(resultCode: Int, data: Intent) {
        cleanupProjection()
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)
        if (projection == null) {
            BotRuntimeBridge.appendLog("VISIÓN", "No se pudo iniciar MediaProjection.")
            stopCaptureAndSelf()
            return
        }

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                BotRuntimeBridge.appendLog("VISIÓN", "Captura de pantalla detenida por el sistema.")
                stopCaptureAndSelf()
            }
        }, null)
        mediaProjection = projection

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val screenW = metrics.widthPixels.coerceAtLeast(720)
        val screenH = metrics.heightPixels.coerceAtLeast(1280)
        val captureW = 540
        val captureH = ((screenH.toFloat() / screenW.toFloat()) * captureW).toInt().coerceAtLeast(900)

        val reader = ImageReader.newInstance(captureW, captureH, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        virtualDisplay = projection.createVirtualDisplay(
            "RoyaleAIVisionDisplay",
            captureW,
            captureH,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null
        )

        BotRuntimeBridge.setCaptureRunning(true)
        BotRuntimeBridge.setOverlayVisible(true)
        BotRuntimeBridge.appendLog(
            tag = "VISIÓN",
            message = "Captura de pantalla activa (${captureW}x${captureH}) sin PC.",
            isHighlight = true
        )

        startAnalysisLoop(screenW, screenH)
    }

    private fun startAnalysisLoop(screenW: Int, screenH: Int) {
        loopJob?.cancel()
        loopJob = serviceScope.launch {
            while (isActive) {
                try {
                    val bitmap = acquireBitmapFromReader()
                    if (bitmap != null) {
                        val cal = BotRuntimeBridge.calibration.value
                        val pkg = BotRuntimeBridge.foregroundPackage.value
                        val vision = visionAnalyzer.analyzeFrame(bitmap, cal, pkg)
                        BotRuntimeBridge.updateVisionState(vision)
                        BotRuntimeBridge.updateLatestBitmap(bitmap)

                        val isPlaying = BotRuntimeBridge.botPlaying.value
                        if (isPlaying) {
                            if (cal.useGeminiVisionLoop &&
                                (vision.battlePhase == BattlePhase.IN_BATTLE ||
                                    vision.battlePhase == BattlePhase.DOUBLE_ELIXIR) &&
                                System.currentTimeMillis() - lastGeminiScanMs > 7_500L
                            ) {
                                triggerGeminiScan(forceNow = false)
                            }

                            val move = tacticalBrain.evaluateNextMove(
                                vision = vision,
                                deck = BotRuntimeBridge.selectedDeck.value,
                                aggression = BotRuntimeBridge.aggressionProfile.value,
                                calibration = cal,
                                geminiReport = BotRuntimeBridge.lastGeminiReport.value
                            )

                            if (move != null) {
                                executeMoveOnScreen(move, screenW, screenH)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in vision loop", e)
                }
                delay(260L)
            }
        }
    }

    private fun executeMoveOnScreen(
        move: com.example.model.TacticalMove,
        screenW: Int,
        screenH: Int
    ) {
        val accessibility = RoyaleAccessibilityService.instance
        if (accessibility == null) {
            BotRuntimeBridge.appendLog(
                "ALERTA",
                "Jugada calculada (${move.cardName}), activa Accesibilidad para tocar pantalla."
            )
            return
        }

        val (realW, realH) = accessibility.getRealScreenSize()
        val w = if (realW > 0) realW else screenW
        val h = if (realH > 0) realH else screenH
        val cal = BotRuntimeBridge.calibration.value

        val (cardPoint, targetPoint) = when (move.source) {
            MoveSource.AUTO_QUEUE -> {
                val btnX = if (move.cardName.contains("OK")) cal.okBtnX else cal.battleBtnX
                val btnY = if (move.cardName.contains("OK")) cal.okBtnY else cal.battleBtnY
                null to PointF(btnX * w, btnY * h)
            }
            else -> {
                val cPt = tacticalBrain.cardSlotToScreenPoint(move.slotIndex, w, h, cal)
                val tPt = tacticalBrain.gridToScreenPoint(move.col, move.row, w, h, cal)
                cPt to tPt
            }
        }

        accessibility.executeTacticalMove(
            move = move,
            cardPoint = cardPoint,
            targetPoint = targetPoint,
            gestureMode = BotRuntimeBridge.gestureMode.value,
            calibration = cal
        )
        BotRuntimeBridge.updateLastMove(move)
        BotRuntimeBridge.appendLog(
            tag = if (move.source == MoveSource.GEMINI_VISION) "GEMINI" else "JUGADA",
            message = "${move.cardName} en ${move.tileLabel}",
            isHighlight = true
        )
    }

    private fun triggerGeminiScan(forceNow: Boolean) {
        if (geminiJob?.isActive == true) return
        val bitmap = BotRuntimeBridge.latestScreenBitmap ?: return
        if (bitmap.isRecycled) return

        val snapshot = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        lastGeminiScanMs = System.currentTimeMillis()

        geminiJob = serviceScope.launch {
            try {
                if (forceNow) {
                    BotRuntimeBridge.appendLog("GEMINI", "Escaneando arena en vivo con Gemini 3.8 Flash...")
                }
                val result = geminiStrategist.analyzeArenaFrame(
                    sourceBitmap = snapshot,
                    selectedDeck = BotRuntimeBridge.selectedDeck.value,
                    onDeviceElixirHint = BotRuntimeBridge.visionState.value.elixir
                )
                result.onSuccess { report ->
                    BotRuntimeBridge.updateGeminiReport(report)
                    BotRuntimeBridge.appendLog(
                        tag = "GEMINI",
                        message = "Rival: ${report.enemyArchetype} | Slot ${report.recommendedSlot} -> (${report.targetCol},${report.targetRow})",
                        isHighlight = true
                    )
                }.onFailure { err ->
                    if (forceNow) {
                        BotRuntimeBridge.appendLog("GEMINI", "Error de análisis: ${err.localizedMessage}")
                    }
                }
            } finally {
                if (!snapshot.isRecycled) {
                    snapshot.recycle()
                }
            }
        }
    }

    private fun acquireBitmapFromReader(): Bitmap? {
        val reader = imageReader ?: return null
        val image = reader.acquireLatestImage() ?: return null
        return try {
            val plane = image.planes.firstOrNull() ?: return null
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val paddedWidth = image.width + rowPadding / pixelStride

            val rawBitmap = Bitmap.createBitmap(
                paddedWidth,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            rawBitmap.copyPixelsFromBuffer(buffer)

            if (paddedWidth != image.width) {
                val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, image.width, image.height)
                rawBitmap.recycle()
                cropped
            } else {
                rawBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed converting Image to Bitmap", e)
            null
        } finally {
            image.close()
        }
    }

    private fun stopCaptureAndSelf() {
        loopJob?.cancel()
        geminiJob?.cancel()
        cleanupProjection()
        BotRuntimeBridge.setCaptureRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupProjection() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        BotRuntimeBridge.onRequestInstantGeminiScan = null
        BotRuntimeBridge.onRequestStopCaptureService = null
        loopJob?.cancel()
        geminiJob?.cancel()
        cleanupProjection()
        BotRuntimeBridge.setCaptureRunning(false)
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "Detener IA", stopPending)
            .build()
    }
}
