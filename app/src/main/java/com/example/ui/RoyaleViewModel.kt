package com.example.ui

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BotPreferencesRepository
import com.example.engine.GeminiVisionStrategist
import com.example.engine.Top1TacticalBrain
import com.example.model.AggressionProfile
import com.example.model.ArenaCalibration
import com.example.model.BotRuntimeBridge
import com.example.model.CalibrationPreset
import com.example.model.DeckArchetype
import com.example.model.GeminiAnalysisUiState
import com.example.model.GestureMode
import com.example.model.TacticalMove
import com.example.service.RoyaleAccessibilityService
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SystemReadinessState(
    val accessibilityEnabled: Boolean = false,
    val notificationGranted: Boolean = true,
    val clashRoyaleInstalled: Boolean = false
)

data class SimulatorState(
    val activeScenario: String = "Montapuercos en Puente Izquierdo",
    val enemyCol: Int = 3,
    val enemyRow: Int = 14,
    val enemyLabel: String = "Montapuercos",
    val counterMove: TacticalMove? = null
)

class RoyaleViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BotPreferencesRepository(application)
    private val geminiStrategist = GeminiVisionStrategist()
    private val tacticalBrain = Top1TacticalBrain()

    val accessibilityConnected = BotRuntimeBridge.accessibilityConnected
    val captureRunning = BotRuntimeBridge.captureRunning
    val botPlaying = BotRuntimeBridge.botPlaying
    val overlayVisible = BotRuntimeBridge.overlayVisible
    val selectedDeck = BotRuntimeBridge.selectedDeck
    val gestureMode = BotRuntimeBridge.gestureMode
    val aggressionProfile = BotRuntimeBridge.aggressionProfile
    val calibration = BotRuntimeBridge.calibration
    val visionState = BotRuntimeBridge.visionState
    val lastMove = BotRuntimeBridge.lastMove
    val lastGeminiReport = BotRuntimeBridge.lastGeminiReport
    val gesturesExecuted = BotRuntimeBridge.gesturesExecuted
    val logs = BotRuntimeBridge.logs

    private val _systemReadiness = MutableStateFlow(SystemReadinessState())
    val systemReadiness: StateFlow<SystemReadinessState> = _systemReadiness.asStateFlow()

    private val _geminiUiState = MutableStateFlow<GeminiAnalysisUiState>(GeminiAnalysisUiState.Idle)
    val geminiUiState: StateFlow<GeminiAnalysisUiState> = _geminiUiState.asStateFlow()

    private val _totalGeminiScans = MutableStateFlow(0)
    val totalGeminiScans: StateFlow<Int> = _totalGeminiScans.asStateFlow()

    private val _simulatorState = MutableStateFlow(
        SimulatorState(
            counterMove = tacticalBrain.simulateProCounter(
                scenarioName = "Montapuercos en Puente Izquierdo",
                enemyCol = 3,
                enemyRow = 14,
                deck = DeckArchetype.HOG_26_CYCLE
            )
        )
    )
    val simulatorState: StateFlow<SimulatorState> = _simulatorState.asStateFlow()

    private val _selectedPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedPreviewBitmap: StateFlow<Bitmap?> = _selectedPreviewBitmap.asStateFlow()

    init {
        viewModelScope.launch {
            repo.configFlow.collect { stored ->
                BotRuntimeBridge.updateDeck(stored.deckArchetype)
                BotRuntimeBridge.updateGestureMode(stored.gestureMode)
                BotRuntimeBridge.updateAggressionProfile(stored.aggressionProfile)
                BotRuntimeBridge.updateCalibration(stored.calibration)
                _totalGeminiScans.value = stored.totalGeminiScans
            }
        }
        refreshSystemReadiness()
    }

    fun refreshSystemReadiness() {
        val ctx = getApplication<Application>()
        val accEnabled = isAccessibilityServiceEnabled(ctx) ||
            (RoyaleAccessibilityService.instance != null)
        BotRuntimeBridge.setAccessibilityConnected(accEnabled)

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val clashInstalled = try {
            ctx.packageManager.getPackageInfo("com.supercell.clashroyale", 0)
            true
        } catch (_: Exception) {
            false
        }

        _systemReadiness.value = SystemReadinessState(
            accessibilityEnabled = accEnabled,
            notificationGranted = notifGranted,
            clashRoyaleInstalled = clashInstalled
        )
    }

    fun selectDeck(deck: DeckArchetype) {
        BotRuntimeBridge.updateDeck(deck)
        viewModelScope.launch { repo.saveDeck(deck) }
        runSimulatorScenario(
            scenarioName = _simulatorState.value.activeScenario,
            enemyCol = _simulatorState.value.enemyCol,
            enemyRow = _simulatorState.value.enemyRow,
            enemyLabel = _simulatorState.value.enemyLabel
        )
    }

    fun selectGestureMode(mode: GestureMode) {
        BotRuntimeBridge.updateGestureMode(mode)
        viewModelScope.launch { repo.saveGestureMode(mode) }
    }

    fun selectAggression(profile: AggressionProfile) {
        BotRuntimeBridge.updateAggressionProfile(profile)
        viewModelScope.launch { repo.saveAggression(profile) }
    }

    fun updateCalibration(updated: ArenaCalibration) {
        BotRuntimeBridge.updateCalibration(updated)
        viewModelScope.launch { repo.saveCalibration(updated) }
    }

    fun applyCalibrationPreset(preset: CalibrationPreset) {
        val current = calibration.value
        val presetCal = ArenaCalibration.fromPreset(preset).copy(
            gestureSpeedMs = current.gestureSpeedMs,
            humanJitterPx = current.humanJitterPx,
            autoQueueEnabled = current.autoQueueEnabled,
            useGeminiVisionLoop = current.useGeminiVisionLoop
        )
        updateCalibration(presetCal)
        BotRuntimeBridge.appendLog("CALIBRACIÓN", "Perfil aplicado: ${preset.label}")
    }

    fun toggleBotPlaying() {
        BotRuntimeBridge.toggleBotPlaying()
    }

    fun setOverlayVisible(visible: Boolean) {
        BotRuntimeBridge.setOverlayVisible(visible)
    }

    fun stopCaptureService() {
        val ctx = getApplication<Application>()
        val stopIntent = Intent(ctx, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        ctx.startService(stopIntent)
        BotRuntimeBridge.setBotPlaying(false)
        BotRuntimeBridge.setCaptureRunning(false)
    }

    fun launchClashRoyale(): Boolean {
        val ctx = getApplication<Application>()
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage("com.supercell.clashroyale")
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(launchIntent)
            BotRuntimeBridge.appendLog("LANZADOR", "Abriendo Clash Royale en el teléfono...", true)
            true
        } else {
            BotRuntimeBridge.appendLog("LANZADOR", "Clash Royale no está instalado en este dispositivo.")
            false
        }
    }

    fun testAccessibilityTapOnCenter() {
        val service = RoyaleAccessibilityService.instance
        if (service == null) {
            BotRuntimeBridge.appendLog("CALIBRACIÓN", "Activa primero el servicio de Accesibilidad.")
            return
        }
        val (w, h) = service.getRealScreenSize()
        val cal = calibration.value
        val pt = tacticalBrain.gridToScreenPoint(9, 20, w, h, cal)
        service.dispatchTap(pt.x, pt.y) { ok ->
            BotRuntimeBridge.appendLog(
                tag = "PRUEBA TÁCTIL",
                message = if (ok) "Toque real ejecutado en (${pt.x.toInt()}, ${pt.y.toInt()}) [Casilla 4-3]."
                else "El gesto de prueba fue cancelado.",
                isHighlight = ok
            )
        }
    }

    fun analyzePhotoUriWithGemini(uri: Uri) {
        viewModelScope.launch {
            _geminiUiState.value = GeminiAnalysisUiState.Loading
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromUri(getApplication(), uri)
                }
                _selectedPreviewBitmap.value = bitmap
                val result = geminiStrategist.analyzeArenaFrame(
                    sourceBitmap = bitmap,
                    selectedDeck = selectedDeck.value,
                    onDeviceElixirHint = visionState.value.elixir
                )
                result.onSuccess { report ->
                    BotRuntimeBridge.updateGeminiReport(report)
                    repo.incrementGeminiScans()
                    _geminiUiState.value = GeminiAnalysisUiState.Success(report)
                    BotRuntimeBridge.appendLog(
                        tag = "GEMINI",
                        message = "Captura analizada: ${report.enemyArchetype} -> Slot ${report.recommendedSlot}",
                        isHighlight = true
                    )
                }.onFailure { err ->
                    _geminiUiState.value = GeminiAnalysisUiState.Error(
                        err.localizedMessage ?: "No se pudo analizar la captura."
                    )
                }
            } catch (e: Exception) {
                _geminiUiState.value = GeminiAnalysisUiState.Error(
                    e.localizedMessage ?: "Error al cargar la imagen."
                )
            }
        }
    }

    fun analyzeLiveFrameWithGemini() {
        val live = BotRuntimeBridge.latestScreenBitmap
        if (live == null || live.isRecycled) {
            _geminiUiState.value = GeminiAnalysisUiState.Error(
                "Activa primero la captura de pantalla en vivo o selecciona una captura desde galería."
            )
            return
        }
        val copy = live.copy(Bitmap.Config.ARGB_8888, false)
        _selectedPreviewBitmap.value = copy

        viewModelScope.launch {
            _geminiUiState.value = GeminiAnalysisUiState.Loading
            val result = geminiStrategist.analyzeArenaFrame(
                sourceBitmap = copy,
                selectedDeck = selectedDeck.value,
                onDeviceElixirHint = visionState.value.elixir
            )
            result.onSuccess { report ->
                BotRuntimeBridge.updateGeminiReport(report)
                repo.incrementGeminiScans()
                _geminiUiState.value = GeminiAnalysisUiState.Success(report)
            }.onFailure { err ->
                _geminiUiState.value = GeminiAnalysisUiState.Error(
                    err.localizedMessage ?: "Error al analizar el frame en vivo."
                )
            }
        }
    }

    fun runSimulatorScenario(
        scenarioName: String,
        enemyCol: Int,
        enemyRow: Int,
        enemyLabel: String
    ) {
        val counter = tacticalBrain.simulateProCounter(
            scenarioName = scenarioName,
            enemyCol = enemyCol,
            enemyRow = enemyRow,
            deck = selectedDeck.value
        )
        _simulatorState.value = SimulatorState(
            activeScenario = scenarioName,
            enemyCol = enemyCol,
            enemyRow = enemyRow,
            enemyLabel = enemyLabel,
            counterMove = counter
        )
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, RoyaleAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any { entry ->
            ComponentName.unflattenFromString(entry) == expectedComponent
        }
    }
}
