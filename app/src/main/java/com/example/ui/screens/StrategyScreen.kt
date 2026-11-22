package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AggressionProfile
import com.example.model.ArenaCalibration
import com.example.model.DeckArchetype
import com.example.model.GestureMode
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElixirMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrategyScreen(
    selectedDeck: DeckArchetype,
    gestureMode: GestureMode,
    aggressionProfile: AggressionProfile,
    calibration: ArenaCalibration,
    onSelectDeck: (DeckArchetype) -> Unit,
    onSelectGestureMode: (GestureMode) -> Unit,
    onSelectAggression: (AggressionProfile) -> Unit,
    onUpdateCalibration: (ArenaCalibration) -> Unit
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
                    text = "ARQUETIPOS PRO TOP-1 LADDER",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalGold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona el mazo y estilo táctico que ejecutará la IA en tu teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        items(DeckArchetype.entries) { deck ->
            val isSelected = deck == selectedDeck
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectDeck(deck) }
                    .testTag("deck_card_${deck.name.lowercase()}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) ObsidianCardElevated else ObsidianCard
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.8.dp else 1.dp,
                    color = if (isSelected) ElectricCyan else ObsidianBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = deck.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = deck.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElixirMagenta.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, ElixirMagenta.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${deck.avgElixir} Elixir",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElixirMagenta,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Seleccionado",
                                    tint = EmeraldReady
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        deck.cards.forEach { cardName ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianDeep,
                                border = BorderStroke(1.dp, ObsidianBorder)
                            ) {
                                Text(
                                    text = cardName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = deck.proSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoyalGold
                    )
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MODO DE CONTROL TÁCTIL",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricCyan
                        )
                    }

                    GestureMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianCardElevated)
                                .clickable { onSelectGestureMode(mode) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("gesture_mode_${mode.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == gestureMode,
                                onClick = { onSelectGestureMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = RoyalGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RITMO Y AUTONOMÍA",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoyalGold
                        )
                    }

                    AggressionProfile.entries.forEach { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianCardElevated)
                                .clickable { onSelectAggression(profile) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("aggression_${profile.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = profile == aggressionProfile,
                                onClick = { onSelectAggression(profile) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${profile.displayName} (ataque >= ${profile.elixirThreshold} elixir)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    ToggleSettingRow(
                        title = "Bucle Estratégico Gemini 3.8 Flash",
                        subtitle = "Analiza la arena cada 7.5s con IA multimodal para leer el mazo rival.",
                        checked = calibration.useGeminiVisionLoop,
                        testTag = "gemini_loop_switch",
                        onCheckedChange = {
                            onUpdateCalibration(calibration.copy(useGeminiVisionLoop = it))
                        }
                    )

                    ToggleSettingRow(
                        title = "Auto-Cola (Batalla / OK automático)",
                        subtitle = "Pulsa Batalla y OK automáticamente al terminar cada partida.",
                        checked = calibration.autoQueueEnabled,
                        testTag = "auto_queue_switch",
                        onCheckedChange = {
                            onUpdateCalibration(calibration.copy(autoQueueEnabled = it))
                        }
                    )

                    ToggleSettingRow(
                        title = "Micro-variación humana (±px)",
                        subtitle = "Añade variación natural en coordenadas y tiempos de toque.",
                        checked = calibration.humanJitterPx > 0,
                        testTag = "human_jitter_switch",
                        onCheckedChange = { enabled ->
                            onUpdateCalibration(calibration.copy(humanJitterPx = if (enabled) 6 else 0))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCardElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
