package com.example.engine

import android.graphics.PointF
import com.example.model.AggressionProfile
import com.example.model.ArenaCalibration
import com.example.model.BattlePhase
import com.example.model.DeckArchetype
import com.example.model.GeminiStrategicReport
import com.example.model.MoveSource
import com.example.model.TacticalMove
import com.example.model.ThreatSide
import com.example.model.VisionFrameState
import kotlin.random.Random

/**
 * Motor táctico inspirado en jugadores Top-1 Ladder de Clash Royale.
 * Usa cuadrícula estándar de arena 18 columnas (0..17) x 32 filas (0..31).
 * Tu lado del campo corresponde a las filas 16..30 (puente en fila 16, torre del rey en fila 28).
 */
class Top1TacticalBrain {

    private var cycleCursor = 0
    private var lastActionTimeMs = 0L

    fun evaluateNextMove(
        vision: VisionFrameState,
        deck: DeckArchetype,
        aggression: AggressionProfile,
        calibration: ArenaCalibration,
        geminiReport: GeminiStrategicReport?,
        nowMs: Long = System.currentTimeMillis()
    ): TacticalMove? {
        // Cooldown mínimo entre gestos para evitar doble toque involuntario
        val minCooldownMs = if (vision.battlePhase == BattlePhase.DOUBLE_ELIXIR) 1050L else 1450L
        if (nowMs - lastActionTimeMs < minCooldownMs) {
            return null
        }

        // 1. Automatización de menú / fin de partida si está activada
        if (calibration.autoQueueEnabled) {
            if (vision.battlePhase == BattlePhase.MATCH_OVER) {
                lastActionTimeMs = nowMs
                return TacticalMove(
                    slotIndex = -1,
                    cardName = "Botón OK",
                    col = 9,
                    row = 31,
                    tileLabel = "Fin de Partida",
                    reason = "Cerrar pantalla de resultados y volver al lobby.",
                    priorityScore = 1.0f,
                    source = MoveSource.AUTO_QUEUE
                )
            }
            if (vision.battlePhase == BattlePhase.IDLE_MENU) {
                lastActionTimeMs = nowMs
                return TacticalMove(
                    slotIndex = -1,
                    cardName = "Botón Batalla",
                    col = 9,
                    row = 29,
                    tileLabel = "Cola Ladder",
                    reason = "Iniciar siguiente partida automáticamente.",
                    priorityScore = 1.0f,
                    source = MoveSource.AUTO_QUEUE
                )
            }
        }

        if (vision.battlePhase != BattlePhase.IN_BATTLE &&
            vision.battlePhase != BattlePhase.DOUBLE_ELIXIR
        ) {
            return null
        }

        val readyIndices = vision.readySlots.mapIndexedNotNull { idx, ready -> if (ready) idx else null }
        if (readyIndices.isEmpty()) return null

        // 2. Priorizar recomendación fresca de Gemini Vision (< 7 segundos) si el slot está listo
        if (geminiReport != null && (nowMs - geminiReport.analyzedAtMs) < 7_000L) {
            val gSlot = (geminiReport.recommendedSlot - 1).coerceIn(0, 3)
            if (vision.readySlots.getOrElse(gSlot) { false } && vision.elixir >= 2.2f) {
                lastActionTimeMs = nowMs
                val col = geminiReport.targetCol.coerceIn(1, 16)
                val row = geminiReport.targetRow.coerceIn(16, 29)
                return TacticalMove(
                    slotIndex = gSlot,
                    cardName = deck.cards.getOrElse(gSlot) { "Carta ${gSlot + 1}" },
                    col = col,
                    row = row,
                    tileLabel = "($col,$row) Gemini",
                    reason = geminiReport.tacticalReason,
                    priorityScore = 0.96f,
                    source = MoveSource.GEMINI_VISION
                )
            }
        }

        val elixir = vision.elixir
        val leftThreat = vision.laneThreat.leftThreat
        val rightThreat = vision.laneThreat.rightThreat
        val maxThreat = maxOf(leftThreat, rightThreat)

        // 3. Defensa de Emergencia (Estructura en 4-3 anti-Rayo / Kite central)
        if (maxThreat >= 0.34f && elixir >= 2.5f) {
            val slot = readyIndices[cycleCursor % readyIndices.size]
            cycleCursor++
            lastActionTimeMs = nowMs

            val isLeftPush = leftThreat >= rightThreat
            val (col, row, tileDesc, tactic) = if (maxThreat >= 0.55f) {
                // Posición clásica Pro 4-3 anti-hechizo para atraer montapuercos/tanques
                val defCol = if (isLeftPush) 9 else 8
                Quad(defCol, 20, "Casilla 4-3 ($defCol,20)", "Defensa 4-3 anti-Rayo para atraer push enemigo al centro")
            } else {
                // Kite central cruzando línea para desviar tropas
                val kiteCol = if (isLeftPush) 9 else 8
                Quad(kiteCol, 17, "Kite Central ($kiteCol,17)", "Kiteo central para desviar unidades y ganar ventaja de elixir")
            }

            return TacticalMove(
                slotIndex = slot,
                cardName = deck.cards.getOrElse(slot) { "Carta ${slot + 1}" },
                col = col,
                row = row,
                tileLabel = tileDesc,
                reason = tactic,
                priorityScore = 0.92f,
                source = MoveSource.ON_DEVICE_PRO
            )
        }

        // 4. Castigo en línea contraria / Presión en el puente cuando tenemos elixir suficiente
        val attackThreshold = if (vision.battlePhase == BattlePhase.DOUBLE_ELIXIR) {
            (aggression.elixirThreshold - 0.8f).coerceAtLeast(4.0f)
        } else {
            aggression.elixirThreshold
        }

        if (elixir >= attackThreshold) {
            val slot = readyIndices[(cycleCursor + 1) % readyIndices.size]
            cycleCursor++
            lastActionTimeMs = nowMs

            // Regla Top-1: si el rival ataca por una línea con poca presión, castiga la contraria
            val pushLeft = when (vision.laneThreat.dominantSide) {
                ThreatSide.LEFT -> false // Castigar derecha
                ThreatSide.RIGHT -> true // Castigar izquierda
                else -> (cycleCursor % 2 == 0)
            }

            val col = if (pushLeft) 3 else 14
            val row = 16 // Justo en el puente (evitando torre central)
            val laneName = if (pushLeft) "Puente Izq" else "Puente Der"

            return TacticalMove(
                slotIndex = slot,
                cardName = deck.cards.getOrElse(slot) { "Carta ${slot + 1}" },
                col = col,
                row = row,
                tileLabel = "$laneName ($col,$row)",
                reason = when (deck) {
                    DeckArchetype.HOG_26_CYCLE -> "Salto de puente rápido aprovechando ventaja de ciclo."
                    DeckArchetype.PEKKA_BRIDGE_SPAM -> "Castigo en línea contraria para dividir el elixir rival."
                    DeckArchetype.MINER_POISON -> "Presión constante para forzar respuesta y desgastar torre."
                    DeckArchetype.LOG_BAIT -> "Presión de baiteo para forzar hechizo pequeño del rival."
                    DeckArchetype.ADAPTIVE_GEMINI -> "Ataque calculado en el puente según ritmo de elixir."
                },
                priorityScore = 0.84f,
                source = MoveSource.ON_DEVICE_PRO
            )
        }

        // 5. Prevención de fuga de elixir (Elixir Leak >= 9.3) -> Rotar detrás de Torre del Rey
        if (elixir >= 9.2f) {
            val slot = readyIndices[cycleCursor % readyIndices.size]
            cycleCursor++
            lastActionTimeMs = nowMs
            val backLeft = (cycleCursor % 2 == 0)
            val col = if (backLeft) 4 else 13
            val row = 28 // Detrás de la torre (anti-hechizo respecto a torre de princesa)
            return TacticalMove(
                slotIndex = slot,
                cardName = deck.cards.getOrElse(slot) { "Carta ${slot + 1}" },
                col = col,
                row = row,
                tileLabel = "Ciclo Atrás ($col,$row)",
                reason = "Evitar desperdicio de elixir (9.2+): ciclo seguro anti-hechizo detrás del Rey.",
                priorityScore = 0.78f,
                source = MoveSource.ON_DEVICE_PRO
            )
        }

        return null
    }

    /**
     * Simulación instantánea para el Laboratorio Táctico dentro de la app.
     */
    fun simulateProCounter(
        scenarioName: String,
        enemyCol: Int,
        enemyRow: Int,
        deck: DeckArchetype
    ): TacticalMove {
        val enemyIsLeft = enemyCol < 9
        return when {
            scenarioName.contains("Barril", ignoreCase = true) -> TacticalMove(
                slotIndex = 3,
                cardName = deck.cards.getOrElse(5) { "El Tronco" },
                col = if (enemyIsLeft) 3 else 14,
                row = 25,
                tileLabel = "Anti-Barril (${if (enemyIsLeft) 3 else 14},25)",
                reason = "Timing perfecto detrás de la torre de princesa para limpiar los 3 duendes sin daño.",
                priorityScore = 0.99f
            )
            scenarioName.contains("Golem", ignoreCase = true) || enemyRow < 10 -> {
                val punishCol = if (enemyIsLeft) 14 else 3
                TacticalMove(
                    slotIndex = 0,
                    cardName = deck.cards.firstOrNull() ?: "Win Condition",
                    col = punishCol,
                    row = 16,
                    tileLabel = "Castigo Opuesto ($punishCol,16)",
                    reason = "El rival gastó 8 de elixir atrás: castigo inmediato en el puente contrario para forzar defensa sin apoyo.",
                    priorityScore = 0.96f
                )
            }
            scenarioName.contains("Montapuercos", ignoreCase = true) -> {
                val defCol = if (enemyIsLeft) 9 else 8
                TacticalMove(
                    slotIndex = 2,
                    cardName = deck.cards.getOrElse(3) { "Estructura" },
                    col = defCol,
                    row = 20,
                    tileLabel = "Pull 4-3 ($defCol,20)",
                    reason = "Colocación 4-3 pro: ambas torres de princesa disparan al Montapuercos y evita valor de Bola de Fuego/Terremoto.",
                    priorityScore = 0.97f
                )
            }
            else -> {
                val kiteCol = if (enemyIsLeft) 9 else 8
                TacticalMove(
                    slotIndex = 1,
                    cardName = deck.cards.getOrElse(2) { "Tanque Ligero" },
                    col = kiteCol,
                    row = 17,
                    tileLabel = "Kite Centro ($kiteCol,17)",
                    reason = "Kiteo al centro para arrastrar tropa pesada hacia la línea opuesta mientras las dos torres disparan.",
                    priorityScore = 0.94f
                )
            }
        }
    }

    fun gridToScreenPoint(
        col: Int,
        row: Int,
        screenWidth: Int,
        screenHeight: Int,
        cal: ArenaCalibration
    ): PointF {
        val normCol = (col.coerceIn(0, 17) + 0.5f) / 18f
        val normRow = (row.coerceIn(0, 31) + 0.5f) / 32f

        val baseX = (cal.arenaLeft + normCol * (cal.arenaRight - cal.arenaLeft)) * screenWidth
        val baseY = (cal.arenaTop + normRow * (cal.arenaBottom - cal.arenaTop)) * screenHeight

        val jitterX = if (cal.humanJitterPx > 0) Random.nextInt(-cal.humanJitterPx, cal.humanJitterPx + 1) else 0
        val jitterY = if (cal.humanJitterPx > 0) Random.nextInt(-cal.humanJitterPx, cal.humanJitterPx + 1) else 0

        return PointF(
            (baseX + jitterX).coerceIn(10f, screenWidth - 10f),
            (baseY + jitterY).coerceIn(10f, screenHeight - 10f)
        )
    }

    fun cardSlotToScreenPoint(
        slotIndex: Int,
        screenWidth: Int,
        screenHeight: Int,
        cal: ArenaCalibration
    ): PointF {
        val normX = cal.cardSlotsX.getOrElse(slotIndex.coerceIn(0, 3)) { 0.5f }
        val baseX = normX * screenWidth
        val baseY = cal.cardsY * screenHeight
        val jitterX = if (cal.humanJitterPx > 0) Random.nextInt(-cal.humanJitterPx, cal.humanJitterPx + 1) else 0
        val jitterY = if (cal.humanJitterPx > 0) Random.nextInt(-cal.humanJitterPx, cal.humanJitterPx + 1) else 0
        return PointF(
            (baseX + jitterX).coerceIn(10f, screenWidth - 10f),
            (baseY + jitterY).coerceIn(10f, screenHeight - 10f)
        )
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
