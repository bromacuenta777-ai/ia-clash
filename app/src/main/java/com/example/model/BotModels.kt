package com.example.model

enum class DeckArchetype(
    val displayName: String,
    val subtitle: String,
    val avgElixir: String,
    val cards: List<String>,
    val proSummary: String
) {
    HOG_26_CYCLE(
        displayName = "Ciclo 2.6 Montapuercos",
        subtitle = "Control y defensa de élite",
        avgElixir = "2.6",
        cards = listOf(
            "Montapuercos", "Mosquetera", "Gólem de Hielo", "Cañón",
            "Bola de Fuego", "El Tronco", "Esqueletos", "Espíritu de Hielo"
        ),
        proSummary = "Cañón en casilla 4-3 anti-Rayo, kite central con Gólem de Hielo y rotación ultra rápida."
    ),
    MINER_POISON(
        displayName = "Minero Veneno Control",
        subtitle = "Desgaste quirúrgico y defensa sólida",
        avgElixir = "3.0",
        cards = listOf(
            "Minero", "Rompemuros", "Veneno", "Torre Tesla",
            "Caballero", "Arqueras", "El Tronco", "Esqueletos"
        ),
        proSummary = "Minero a la esquina segura anti-Tornado + Veneno sobre tropas en torre + Tesla central."
    ),
    PEKKA_BRIDGE_SPAM(
        displayName = "P.E.K.K.A Bridge Spam",
        subtitle = "Contraataque y castigo doble línea",
        avgElixir = "3.9",
        cards = listOf(
            "P.E.K.K.A", "Ariete de Batalla", "Bandida", "Fantasma Real",
            "Mago Eléctrico", "Arquero Mágico", "Veneno", "Descarga"
        ),
        proSummary = "Defiende con P.E.K.K.A atrás y castiga línea contraria en el puente cuando el rival gaste elixir."
    ),
    LOG_BAIT(
        displayName = "Log Bait Clásico",
        subtitle = "Baiteo de hechizos y ciclo de Cohete",
        avgElixir = "3.3",
        cards = listOf(
            "Barril de Duendes", "Princesa", "Pandilla de Duendes", "Caballero",
            "Torre Infernal", "Cohete", "El Tronco", "Espíritu de Hielo"
        ),
        proSummary = "Princesa en el puente o atrás, fuerza el Tronco enemigo y cierra en elixir x2 con Cohete."
    ),
    ADAPTIVE_GEMINI(
        displayName = "Gemini Adaptativo",
        subtitle = "Visión multimodal en tiempo real",
        avgElixir = "Auto",
        cards = listOf(
            "Slot 1 (Auto)", "Slot 2 (Auto)", "Slot 3 (Auto)", "Slot 4 (Auto)",
            "Lectura Visión", "Conteo Elixir", "Anti-Hechizo", "Macro Pro"
        ),
        proSummary = "Gemini 3.8 Flash inspecciona tus cartas y el mazo rival para ajustar la táctica dinámicamente."
    )
}

enum class GestureMode(val displayName: String, val description: String) {
    TAP_THEN_PLACE("Toque + Casilla", "Toca la carta y luego toca la casilla exacta de la arena (más rápido y fiable)."),
    DRAG_AND_DROP("Arrastrar Carta", "Desliza suavemente desde el slot de carta hasta la casilla objetivo.")
}

enum class AggressionProfile(val displayName: String, val elixirThreshold: Float) {
    DEFENSIVE_COUNTER("Defensivo / Contraataque", 7.8f),
    BALANCED_PRO("Equilibrado Top Ladder", 6.5f),
    AGGRESSIVE_PRESSURE("Presión Constante", 5.2f)
}

enum class CalibrationPreset(val label: String) {
    PHONE_20_9("Pantalla 20:9 (Moderna)"),
    PHONE_19_5_9("Pantalla 19.5:9"),
    PHONE_16_9("Pantalla 16:9")
}

data class ArenaCalibration(
    val arenaLeft: Float = 0.08f,
    val arenaTop: Float = 0.14f,
    val arenaRight: Float = 0.92f,
    val arenaBottom: Float = 0.74f,
    val elixirBarY: Float = 0.955f,
    val elixirBarLeft: Float = 0.25f,
    val elixirBarRight: Float = 0.93f,
    val cardsY: Float = 0.86f,
    val cardSlotsX: List<Float> = listOf(0.28f, 0.46f, 0.64f, 0.82f),
    val battleBtnX: Float = 0.50f,
    val battleBtnY: Float = 0.78f,
    val okBtnX: Float = 0.50f,
    val okBtnY: Float = 0.88f,
    val gestureSpeedMs: Long = 115L,
    val humanJitterPx: Int = 6,
    val autoQueueEnabled: Boolean = false,
    val useGeminiVisionLoop: Boolean = true
) {
    companion object {
        fun fromPreset(preset: CalibrationPreset): ArenaCalibration = when (preset) {
            CalibrationPreset.PHONE_20_9 -> ArenaCalibration()
            CalibrationPreset.PHONE_19_5_9 -> ArenaCalibration(
                arenaTop = 0.13f,
                arenaBottom = 0.75f,
                elixirBarY = 0.95f,
                cardsY = 0.855f
            )
            CalibrationPreset.PHONE_16_9 -> ArenaCalibration(
                arenaLeft = 0.06f,
                arenaTop = 0.11f,
                arenaRight = 0.94f,
                arenaBottom = 0.76f,
                elixirBarY = 0.96f,
                cardsY = 0.865f
            )
        }
    }
}

enum class BattlePhase(val labelEs: String) {
    IDLE_MENU("En Menú / Cola"),
    IN_BATTLE("En Batalla (x1)"),
    DOUBLE_ELIXIR("Elixir Doble / Muerte Súbita"),
    MATCH_OVER("Fin de Partida"),
    UNKNOWN("Escaneando Pantalla")
}

enum class ThreatSide(val labelEs: String) {
    NONE("Sin Amenaza"),
    LEFT("Presión Izquierda"),
    RIGHT("Presión Derecha"),
    BOTH("Ataque Doble Línea")
}

data class LaneThreat(
    val leftThreat: Float = 0f,
    val rightThreat: Float = 0f,
    val leftFriendly: Float = 0f,
    val rightFriendly: Float = 0f,
    val dominantSide: ThreatSide = ThreatSide.NONE
)

data class VisionFrameState(
    val timestampMs: Long = 0L,
    val elixir: Float = 0f,
    val readySlots: List<Boolean> = listOf(true, true, true, true),
    val battlePhase: BattlePhase = BattlePhase.UNKNOWN,
    val laneThreat: LaneThreat = LaneThreat(),
    val foregroundPackage: String? = null
)

enum class MoveSource {
    ON_DEVICE_PRO,
    GEMINI_VISION,
    AUTO_QUEUE
}

data class TacticalMove(
    val slotIndex: Int,
    val cardName: String,
    val col: Int,
    val row: Int,
    val tileLabel: String,
    val reason: String,
    val priorityScore: Float,
    val source: MoveSource = MoveSource.ON_DEVICE_PRO
)

data class GeminiStrategicReport(
    val detectedPhase: String,
    val estimatedElixir: Double,
    val enemyArchetype: String,
    val threatLane: String,
    val recommendedSlot: Int,
    val targetCol: Int,
    val targetRow: Int,
    val tacticalReason: String,
    val macroAdvice: String,
    val confidence: String,
    val analyzedAtMs: Long = System.currentTimeMillis()
)

data class TelemetryLogEntry(
    val id: Long,
    val timeLabel: String,
    val tag: String,
    val message: String,
    val isHighlight: Boolean = false
)

sealed interface GeminiAnalysisUiState {
    data object Idle : GeminiAnalysisUiState
    data object Loading : GeminiAnalysisUiState
    data class Success(val report: GeminiStrategicReport) : GeminiAnalysisUiState
    data class Error(val message: String) : GeminiAnalysisUiState
}
