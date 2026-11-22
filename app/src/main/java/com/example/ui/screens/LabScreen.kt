package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DeckArchetype
import com.example.model.GeminiAnalysisUiState
import com.example.ui.SimulatorState
import com.example.ui.components.ArenaGridCanvas
import com.example.ui.theme.CrimsonThreat
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabScreen(
    selectedDeck: DeckArchetype,
    simulatorState: SimulatorState,
    geminiUiState: GeminiAnalysisUiState,
    previewBitmap: Bitmap?,
    totalGeminiScans: Int,
    onRunSimulatorScenario: (scenarioName: String, col: Int, row: Int, label: String) -> Unit,
    onAnalyzePhotoUri: (android.net.Uri) -> Unit,
    onAnalyzeLiveFrame: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAnalyzePhotoUri(uri)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VISIÓN MULTIMODAL GEMINI 3.8",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianDeep,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Text(
                                text = "Escaneos: $totalGeminiScans",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Sube una captura de partida o analiza el frame en vivo para recibir lectura de mazo rival, elixir y jugada exacta Top-1.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_screenshot_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Elegir Captura")
                        }

                        OutlinedButton(
                            onClick = onAnalyzeLiveFrame,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("analyze_live_frame_button"),
                            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold)
                        ) {
                            Icon(Icons.Filled.Screenshot, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Frame en Vivo")
                        }
                    }

                    if (previewBitmap != null && !previewBitmap.isRecycled) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Captura analizada por Gemini",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    when (geminiUiState) {
                        is GeminiAnalysisUiState.Idle -> Unit
                        is GeminiAnalysisUiState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = ElectricCyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Analizando tablero con Gemini 3.8 Flash...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ElectricCyan
                                )
                            }
                        }
                        is GeminiAnalysisUiState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CrimsonThreat.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, CrimsonThreat.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = geminiUiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CrimsonThreat,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        is GeminiAnalysisUiState.Success -> {
                            val r = geminiUiState.report
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCardElevated,
                                border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "FASE: ${r.detectedPhase}  •  ELIXIR: ${"%.1f".format(r.estimatedElixir)}  •  CONFIANZA: ${r.confidence}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldReady
                                    )
                                    Text(
                                        text = "Rival probable: ${r.enemyArchetype} (Amenaza: ${r.threatLane})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = RoyalGold
                                    )
                                    Text(
                                        text = "Jugada Top-1: Carta Slot ${r.recommendedSlot} -> Casilla (${r.targetCol}, ${r.targetRow})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = r.tacticalReason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Macro 15s: ${r.macroAdvice}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
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
                    Text(
                        text = "SIMULADOR TÁCTICO TOP-1 (18x32)",
                        style = MaterialTheme.typography.titleMedium,
                        color = RoyalGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toca cualquier casilla de la arena o elige un ataque rival para ver la respuesta exacta del cerebro (${selectedDeck.displayName}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val scenarios = listOf(
                            ScenarioSpec("Montapuercos en Puente", 3, 14, "Montapuercos"),
                            ScenarioSpec("Golem Atrás", 4, 4, "Golem"),
                            ScenarioSpec("Barril de Duendes", 3, 25, "Barril"),
                            ScenarioSpec("Megacaballero", 14, 15, "Megacaballero")
                        )
                        scenarios.forEach { s ->
                            FilterChip(
                                selected = simulatorState.activeScenario == s.title,
                                onClick = {
                                    onRunSimulatorScenario(s.title, s.col, s.row, s.label)
                                },
                                label = { Text(s.title) },
                                modifier = Modifier.testTag("sim_chip_${s.label.lowercase()}")
                            )
                        }
                    }

                    ArenaGridCanvas(
                        enemyCol = simulatorState.enemyCol,
                        enemyRow = simulatorState.enemyRow,
                        targetCol = simulatorState.counterMove?.col,
                        targetRow = simulatorState.counterMove?.row,
                        onTileTap = { col, row ->
                            onRunSimulatorScenario(
                                "Amenaza en ($col,$row)",
                                col,
                                row,
                                "Tropa Rival"
                            )
                        }
                    )

                    simulatorState.counterMove?.let { move ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianCardElevated,
                            border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "RESPUESTA ÓPTIMA: ${move.cardName} -> ${move.tileLabel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldReady
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = move.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ScenarioSpec(
    val title: String,
    val col: Int,
    val row: Int,
    val label: String
)
