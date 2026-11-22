package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.ArenaCalibration
import com.example.model.BattlePhase
import com.example.model.LaneThreat
import com.example.model.ThreatSide
import com.example.model.VisionFrameState

class ArenaVisionAnalyzer {

    fun analyzeFrame(
        bitmap: Bitmap,
        calibration: ArenaCalibration,
        foregroundPackage: String?
    ): VisionFrameState {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 64 || h < 64) {
            return VisionFrameState(
                timestampMs = System.currentTimeMillis(),
                foregroundPackage = foregroundPackage
            )
        }

        val elixir = estimateElixir(bitmap, w, h, calibration)
        val readySlots = detectReadyCardSlots(bitmap, w, h, calibration)
        val laneThreat = scanArenaLanes(bitmap, w, h, calibration)
        val battlePhase = detectBattlePhase(bitmap, w, h, calibration, elixir)

        return VisionFrameState(
            timestampMs = System.currentTimeMillis(),
            elixir = elixir,
            readySlots = readySlots,
            battlePhase = battlePhase,
            laneThreat = laneThreat,
            foregroundPackage = foregroundPackage
        )
    }

    private fun estimateElixir(
        bitmap: Bitmap,
        w: Int,
        h: Int,
        cal: ArenaCalibration
    ): Float {
        val startX = (cal.elixirBarLeft * w).toInt().coerceIn(0, w - 2)
        val endX = (cal.elixirBarRight * w).toInt().coerceIn(startX + 1, w - 1)
        val yCenter = (cal.elixirBarY * h).toInt().coerceIn(1, h - 2)

        val steps = 40
        val stepPx = ((endX - startX).toFloat() / steps).coerceAtLeast(1f)
        var filledSteps = 0

        for (i in 0 until steps) {
            val x = (startX + i * stepPx).toInt().coerceIn(0, w - 1)
            var isElixirSample = false
            for (dy in -2..2) {
                val py = (yCenter + dy).coerceIn(0, h - 1)
                val pixel = bitmap.getPixel(x, py)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Clash Royale Elixir Bar magenta/violet signature
                if (r > 135 && b > 145 && g < 130 && (r + b - 2 * g) > 105) {
                    isElixirSample = true
                    break
                }
            }
            if (isElixirSample) {
                filledSteps = i + 1
            }
        }

        val ratio = filledSteps.toFloat() / steps.toFloat()
        return (ratio * 10f).coerceIn(0f, 10f)
    }

    private fun detectReadyCardSlots(
        bitmap: Bitmap,
        w: Int,
        h: Int,
        cal: ArenaCalibration
    ): List<Boolean> {
        val cy = (cal.cardsY * h).toInt().coerceIn(2, h - 3)
        return cal.cardSlotsX.map { normX ->
            val cx = (normX * w).toInt().coerceIn(4, w - 5)
            var saturationSum = 0f
            var lumaSum = 0f
            var count = 0
            for (dx in -4..4 step 2) {
                for (dy in -4..4 step 2) {
                    val pixel = bitmap.getPixel((cx + dx).coerceIn(0, w - 1), (cy + dy).coerceIn(0, h - 1))
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val maxC = maxOf(r, g, b)
                    val minC = minOf(r, g, b)
                    saturationSum += (maxC - minC)
                    lumaSum += (r + g + b) / 3f
                    count++
                }
            }
            val avgSat = if (count > 0) saturationSum / count else 0f
            val avgLuma = if (count > 0) lumaSum / count else 0f
            // Active cards have vibrant color/brightness compared to greyed-out cards
            avgSat > 24f && avgLuma > 68f
        }
    }

    private fun scanArenaLanes(
        bitmap: Bitmap,
        w: Int,
        h: Int,
        cal: ArenaCalibration
    ): LaneThreat {
        val leftX = (cal.arenaLeft * w).toInt().coerceIn(0, w - 2)
        val rightX = (cal.arenaRight * w).toInt().coerceIn(leftX + 2, w - 1)
        val midX = (leftX + rightX) / 2
        val topY = (cal.arenaTop * h).toInt().coerceIn(0, h - 2)
        val bottomY = (cal.arenaBottom * h).toInt().coerceIn(topY + 2, h - 1)

        var leftRed = 0
        var rightRed = 0
        var leftBlue = 0
        var rightBlue = 0
        var leftSamples = 0
        var rightSamples = 0

        val stepX = ((rightX - leftX) / 24).coerceAtLeast(4)
        val stepY = ((bottomY - topY) / 30).coerceAtLeast(4)

        var y = topY
        while (y < bottomY) {
            val isDefensiveHalf = y > (topY + (bottomY - topY) * 0.45f)
            val weight = if (isDefensiveHalf) 2 else 1

            var x = leftX
            while (x < rightX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val isEnemyRed = r > 185 && g < 85 && b < 95 && (r - g) > 105
                val isFriendlyBlue = b > 175 && r < 95 && g in 70..195 && (b - r) > 85

                if (x < midX) {
                    leftSamples++
                    if (isEnemyRed) leftRed += weight
                    if (isFriendlyBlue) leftBlue += 1
                } else {
                    rightSamples++
                    if (isEnemyRed) rightRed += weight
                    if (isFriendlyBlue) rightBlue += 1
                }
                x += stepX
            }
            y += stepY
        }

        val leftThreat = ((leftRed.toFloat() / maxOf(1, leftSamples)) * 18f).coerceIn(0f, 1f)
        val rightThreat = ((rightRed.toFloat() / maxOf(1, rightSamples)) * 18f).coerceIn(0f, 1f)
        val leftFriendly = ((leftBlue.toFloat() / maxOf(1, leftSamples)) * 18f).coerceIn(0f, 1f)
        val rightFriendly = ((rightBlue.toFloat() / maxOf(1, rightSamples)) * 18f).coerceIn(0f, 1f)

        val dominantSide = when {
            leftThreat > 0.38f && rightThreat > 0.38f -> ThreatSide.BOTH
            leftThreat > 0.25f && leftThreat >= rightThreat + 0.08f -> ThreatSide.LEFT
            rightThreat > 0.25f && rightThreat >= leftThreat + 0.08f -> ThreatSide.RIGHT
            leftThreat > 0.28f -> ThreatSide.LEFT
            rightThreat > 0.28f -> ThreatSide.RIGHT
            else -> ThreatSide.NONE
        }

        return LaneThreat(
            leftThreat = leftThreat,
            rightThreat = rightThreat,
            leftFriendly = leftFriendly,
            rightFriendly = rightFriendly,
            dominantSide = dominantSide
        )
    }

    private fun detectBattlePhase(
        bitmap: Bitmap,
        w: Int,
        h: Int,
        cal: ArenaCalibration,
        elixir: Float
    ): BattlePhase {
        if (elixir >= 0.4f) {
            // Check upper-right timer area for overtime / double elixir pinkish-gold flash
            val timerX = (0.85f * w).toInt().coerceIn(0, w - 1)
            val timerY = (0.07f * h).toInt().coerceIn(0, h - 1)
            val p = bitmap.getPixel(timerX, timerY)
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val isDoubleOrOvertime = r > 170 && b > 140 && g < 130
            return if (isDoubleOrOvertime) BattlePhase.DOUBLE_ELIXIR else BattlePhase.IN_BATTLE
        }

        // Check OK button at bottom center (blue/gold CTA on end-of-match screen)
        val okX = (cal.okBtnX * w).toInt().coerceIn(0, w - 1)
        val okY = (cal.okBtnY * h).toInt().coerceIn(0, h - 1)
        val okPixel = bitmap.getPixel(okX, okY)
        val okBlue = Color.blue(okPixel) > 160 && Color.red(okPixel) < 110
        if (okBlue) {
            return BattlePhase.MATCH_OVER
        }

        // Check Battle button (gold/amber CTA on main lobby)
        val batX = (cal.battleBtnX * w).toInt().coerceIn(0, w - 1)
        val batY = (cal.battleBtnY * h).toInt().coerceIn(0, h - 1)
        val batPixel = bitmap.getPixel(batX, batY)
        val isGoldButton = Color.red(batPixel) > 190 && Color.green(batPixel) > 130 && Color.blue(batPixel) < 90
        if (isGoldButton) {
            return BattlePhase.IDLE_MENU
        }

        return BattlePhase.UNKNOWN
    }
}
