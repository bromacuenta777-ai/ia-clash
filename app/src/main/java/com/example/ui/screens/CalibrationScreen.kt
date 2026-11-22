package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ArenaCalibration
import com.example.model.CalibrationPreset
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElixirMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalibrationScreen(
    calibration: ArenaCalibration,
    accessibilityConnected: Boolean,
    onApplyPreset: (CalibrationPreset) -> Unit,
    onUpdateCalibration: (ArenaCalibration) -> Unit,
    onTestTap: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Text(
                    text = "CALIBRACIÓN DE PANTALLA",
                    style = MaterialTheme.typography.titleLarge,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ajusta la zona exacta de la arena, cartas y barra de elixir según la proporción de tu teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MAPA PROPORCIONAL DEL TELÉFONO",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalGold
                    )

                    Box(
                        modifier = Modifier
                            .width(210.dp)
                            .aspectRatio(9f / 19.5f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(ObsidianDeep)
                            .border(2.dp, ObsidianBorder, RoundedCornerShape(22.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Arena Rect
                            val aLeft = calibration.arenaLeft * w
                            val aTop = calibration.arenaTop * h
                            val aWidth = (calibration.arenaRight - calibration.arenaLeft) * w
                            val aHeight = (calibration.arenaBottom - calibration.arenaTop) * h

                            drawRect(
                                color = ElectricCyan.copy(alpha = 0.14f),
                                topLeft = Offset(aLeft, aTop),
                                size = Size(aWidth, aHeight)
                            )
                            drawRect(
                                color = ElectricCyan,
                                topLeft = Offset(aLeft, aTop),
                                size = Size(aWidth, aHeight),
                                style = Stroke(width = 2.5f)
                            )

                            // River line inside arena
                            val riverY = aTop + aHeight * 0.5f
                            drawLine(
                                color = ElectricCyan.copy(alpha = 0.55f),
                                start = Offset(aLeft, riverY),
                                end = Offset(aLeft + aWidth, riverY),
                                strokeWidth = 2f
                            )

                            // 4 Card slots
                            val cardY = calibration.cardsY * h
                            calibration.cardSlotsX.forEach { slotX ->
                                drawCircle(
                                    color = RoyalGold,
                                    radius = w * 0.065f,
                                    center = Offset(slotX * w, cardY),
                                    style = Stroke(width = 2.5f)
                                )
                            }

                            // Elixir bar
                            val elixY = calibration.elixirBarY * h
                            drawLine(
                                color = ElixirMagenta,
                                start = Offset(calibration.elixirBarLeft * w, elixY),
                                end = Offset(calibration.elixirBarRight * w, elixY),
                                strokeWidth = 8f
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalibrationPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = false,
                                onClick = { onApplyPreset(preset) },
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag("preset_${preset.name.lowercase()}")
                            )
                        }
                    }

                    Button(
                        onClick = onTestTap,
                        enabled = accessibilityConnected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_tap_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldReady,
                            contentColor = ObsidianDeep
                        )
                    ) {
                        Icon(Icons.Filled.TouchApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (accessibilityConnected) "Probar Toque Real (Casilla 4-3)"
                            else "Activa Accesibilidad para Probar Toque"
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalibrationSlider(
                        label = "Límite Superior Arena",
                        value = calibration.arenaTop,
                        range = 0.06f..0.28f,
                        valueText = "${(calibration.arenaTop * 100).toInt()}%",
                        onValueChange = { onUpdateCalibration(calibration.copy(arenaTop = it)) }
                    )
                    CalibrationSlider(
                        label = "Límite Inferior Arena",
                        value = calibration.arenaBottom,
                        range = 0.62f..0.82f,
                        valueText = "${(calibration.arenaBottom * 100).toInt()}%",
                        onValueChange = { onUpdateCalibration(calibration.copy(arenaBottom = it)) }
                    )
                    CalibrationSlider(
                        label = "Altura Fila de Cartas",
                        value = calibration.cardsY,
                        range = 0.78f..0.92f,
                        valueText = "${(calibration.cardsY * 100).toInt()}%",
                        onValueChange = { onUpdateCalibration(calibration.copy(cardsY = it)) }
                    )
                    CalibrationSlider(
                        label = "Altura Barra de Elixir",
                        value = calibration.elixirBarY,
                        range = 0.90f..0.985f,
                        valueText = "${(calibration.elixirBarY * 100).toInt()}%",
                        onValueChange = { onUpdateCalibration(calibration.copy(elixirBarY = it)) }
                    )
                    CalibrationSlider(
                        label = "Velocidad de Gesto (ms)",
                        value = calibration.gestureSpeedMs.toFloat(),
                        range = 70f..260f,
                        valueText = "${calibration.gestureSpeedMs} ms",
                        onValueChange = { onUpdateCalibration(calibration.copy(gestureSpeedMs = it.toLong())) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCyan
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = RoyalGold,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = ObsidianDeep
            )
        )
    }
}
