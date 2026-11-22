package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CrimsonThreat
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElixirMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun StatusPillBadge(
    label: String,
    isActive: Boolean,
    activeColor: Color = EmeraldReady,
    inactiveColor: Color = TextMuted,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        label = "status_color"
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ElixirTelemetryBar(
    elixir: Float,
    modifier: Modifier = Modifier
) {
    val animatedElixir by animateFloatAsState(
        targetValue = elixir.coerceIn(0f, 10f),
        animationSpec = spring(),
        label = "elixir_anim"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MEDIDOR DE ELIXIR EN VIVO",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = "${"%.1f".format(animatedElixir)} / 10.0",
                style = MaterialTheme.typography.labelMedium,
                color = ElixirMagenta,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ObsidianDeep)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (drop in 1..10) {
                val fill = (animatedElixir - (drop - 1)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(3.dp))
                        .background(ObsidianCard)
                ) {
                    if (fill > 0.02f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fill)
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFAB47BC), ElixirMagenta)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArenaGridCanvas(
    enemyCol: Int?,
    enemyRow: Int?,
    targetCol: Int?,
    targetRow: Int?,
    modifier: Modifier = Modifier,
    onTileTap: ((col: Int, row: Int) -> Unit)? = null
) {
    val tapModifier = if (onTileTap != null) {
        Modifier.pointerInput(onTileTap) {
            detectTapGestures { offset ->
                val col = ((offset.x / size.width) * 18f).toInt().coerceIn(0, 17)
                val row = ((offset.y / size.height) * 32f).toInt().coerceIn(0, 31)
                onTileTap(col, row)
            }
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(18f / 26f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A1322))
            .border(1.5.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .then(tapModifier)
    ) {
        val w = size.width
        val h = size.height
        val cellW = w / 18f
        val cellH = h / 32f

        // Checkerboard tactical grass
        for (r in 0 until 32) {
            for (c in 0 until 18) {
                val isLight = (r + c) % 2 == 0
                val tileColor = when {
                    r in 15..16 -> Color(0xFF0E2942) // River
                    r < 15 -> if (isLight) Color(0xFF131E30) else Color(0xFF101929)
                    else -> if (isLight) Color(0xFF15253A) else Color(0xFF111E31)
                }
                drawRect(
                    color = tileColor,
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW + 0.5f, cellH + 0.5f)
                )
            }
        }

        // Bridges at cols 2..4 and 13..15, rows 15..16
        val bridgeColor = Color(0xFF4A3B28)
        drawRect(
            color = bridgeColor,
            topLeft = Offset(2.5f * cellW, 14.8f * cellH),
            size = Size(2.4f * cellW, 2.4f * cellH)
        )
        drawRect(
            color = bridgeColor,
            topLeft = Offset(13.1f * cellW, 14.8f * cellH),
            size = Size(2.4f * cellW, 2.4f * cellH)
        )

        // Enemy Towers (Top)
        drawTowerBlock(3.5f * cellW, 6.5f * cellH, cellW * 2.4f, CrimsonThreat)
        drawTowerBlock(14.5f * cellW, 6.5f * cellH, cellW * 2.4f, CrimsonThreat)
        drawTowerBlock(9.0f * cellW, 2.8f * cellH, cellW * 3.0f, CrimsonThreat)

        // Friendly Towers (Bottom)
        drawTowerBlock(3.5f * cellW, 25.0f * cellH, cellW * 2.4f, ElectricCyan)
        drawTowerBlock(14.5f * cellW, 25.0f * cellH, cellW * 2.4f, ElectricCyan)
        drawTowerBlock(9.0f * cellW, 28.5f * cellH, cellW * 3.0f, RoyalGold)

        // Dotted dashed trajectory from enemy to counter or bridge
        if (enemyCol != null && enemyRow != null && targetCol != null && targetRow != null) {
            val ePt = Offset((enemyCol + 0.5f) * cellW, (enemyRow + 0.5f) * cellH)
            val tPt = Offset((targetCol + 0.5f) * cellW, (targetRow + 0.5f) * cellH)
            drawLine(
                color = RoyalGold.copy(alpha = 0.65f),
                start = ePt,
                end = tPt,
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        }

        // Enemy Marker
        if (enemyCol != null && enemyRow != null) {
            val center = Offset((enemyCol + 0.5f) * cellW, (enemyRow + 0.5f) * cellH)
            drawCircle(
                color = CrimsonThreat.copy(alpha = 0.28f),
                radius = cellW * 1.45f,
                center = center
            )
            drawCircle(
                color = CrimsonThreat,
                radius = cellW * 0.72f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = cellW * 0.72f,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }

        // Pro AI Target Marker
        if (targetCol != null && targetRow != null) {
            val center = Offset((targetCol + 0.5f) * cellW, (targetRow + 0.5f) * cellH)
            drawCircle(
                color = EmeraldReady.copy(alpha = 0.24f),
                radius = cellW * 1.8f,
                center = center
            )
            drawCircle(
                color = ElectricCyan,
                radius = cellW * 1.15f,
                center = center,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = EmeraldReady,
                radius = cellW * 0.65f,
                center = center
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTowerBlock(
    cx: Float,
    cy: Float,
    boxSize: Float,
    accent: Color
) {
    val half = boxSize / 2f
    drawRoundRect(
        color = accent.copy(alpha = 0.25f),
        topLeft = Offset(cx - half, cy - half),
        size = Size(boxSize, boxSize),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = accent,
        topLeft = Offset(cx - half, cy - half),
        size = Size(boxSize, boxSize),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2.5f)
    )
}
