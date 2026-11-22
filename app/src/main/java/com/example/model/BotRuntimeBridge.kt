package com.example.model

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object BotRuntimeBridge {
    private val logCounter = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _accessibilityConnected = MutableStateFlow(false)
    val accessibilityConnected: StateFlow<Boolean> = _accessibilityConnected.asStateFlow()

    private val _captureRunning = MutableStateFlow(false)
    val captureRunning: StateFlow<Boolean> = _captureRunning.asStateFlow()

    private val _botPlaying = MutableStateFlow(false)
    val botPlaying: StateFlow<Boolean> = _botPlaying.asStateFlow()

    private val _overlayVisible = MutableStateFlow(true)
    val overlayVisible: StateFlow<Boolean> = _overlayVisible.asStateFlow()

    private val _selectedDeck = MutableStateFlow(DeckArchetype.HOG_26_CYCLE)
    val selectedDeck: StateFlow<DeckArchetype> = _selectedDeck.asStateFlow()

    private val _gestureMode = MutableStateFlow(GestureMode.TAP_THEN_PLACE)
    val gestureMode: StateFlow<GestureMode> = _gestureMode.asStateFlow()

    private val _aggressionProfile = MutableStateFlow(AggressionProfile.BALANCED_PRO)
    val aggressionProfile: StateFlow<AggressionProfile> = _aggressionProfile.asStateFlow()

    private val _calibration = MutableStateFlow(ArenaCalibration())
    val calibration: StateFlow<ArenaCalibration> = _calibration.asStateFlow()

    private val _visionState = MutableStateFlow(VisionFrameState())
    val visionState: StateFlow<VisionFrameState> = _visionState.asStateFlow()

    private val _lastMove = MutableStateFlow<TacticalMove?>(null)
    val lastMove: StateFlow<TacticalMove?> = _lastMove.asStateFlow()

    private val _lastGeminiReport = MutableStateFlow<GeminiStrategicReport?>(null)
    val lastGeminiReport: StateFlow<GeminiStrategicReport?> = _lastGeminiReport.asStateFlow()

    private val _gesturesExecuted = MutableStateFlow(0)
    val gesturesExecuted: StateFlow<Int> = _gesturesExecuted.asStateFlow()

    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    private val _logs = MutableStateFlow<List<TelemetryLogEntry>>(
        listOf(
            TelemetryLogEntry(
                id = 0L,
                timeLabel = "--:--:--",
                tag = "SISTEMA",
                message = "Motor táctico RoyaleAI inicializado y listo en dispositivo.",
                isHighlight = false
            )
        )
    )
    val logs: StateFlow<List<TelemetryLogEntry>> = _logs.asStateFlow()

    @Volatile
    var latestScreenBitmap: Bitmap? = null
        private set

    @Volatile
    var onRequestInstantGeminiScan: (() -> Unit)? = null

    @Volatile
    var onRequestStopCaptureService: (() -> Unit)? = null

    fun setAccessibilityConnected(connected: Boolean) {
        _accessibilityConnected.value = connected
    }

    fun setCaptureRunning(running: Boolean) {
        _captureRunning.value = running
        if (!running) {
            _botPlaying.value = false
        }
    }

    fun setBotPlaying(playing: Boolean) {
        _botPlaying.value = playing
        appendLog(
            tag = "MOTOR",
            message = if (playing) "IA autónoma ACTIVADA: tomando control táctico." else "IA autónoma en PAUSA.",
            isHighlight = playing
        )
    }

    fun toggleBotPlaying() {
        setBotPlaying(!_botPlaying.value)
    }

    fun setOverlayVisible(visible: Boolean) {
        _overlayVisible.value = visible
    }

    fun updateDeck(deck: DeckArchetype) {
        _selectedDeck.value = deck
    }

    fun updateGestureMode(mode: GestureMode) {
        _gestureMode.value = mode
    }

    fun updateAggressionProfile(profile: AggressionProfile) {
        _aggressionProfile.value = profile
    }

    fun updateCalibration(calibration: ArenaCalibration) {
        _calibration.value = calibration
    }

    fun updateVisionState(state: VisionFrameState) {
        _visionState.value = state
    }

    fun updateLastMove(move: TacticalMove) {
        _lastMove.value = move
        _gesturesExecuted.update { it + 1 }
    }

    fun updateGeminiReport(report: GeminiStrategicReport) {
        _lastGeminiReport.value = report
    }

    fun updateForegroundPackage(packageName: String?) {
        _foregroundPackage.value = packageName
    }

    fun updateLatestBitmap(bitmap: Bitmap) {
        val old = latestScreenBitmap
        latestScreenBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            old.recycle()
        }
    }

    fun appendLog(tag: String, message: String, isHighlight: Boolean = false) {
        val entry = TelemetryLogEntry(
            id = logCounter.getAndIncrement(),
            timeLabel = timeFormat.format(Date()),
            tag = tag,
            message = message,
            isHighlight = isHighlight
        )
        _logs.update { current ->
            (listOf(entry) + current).take(60)
        }
    }
}
