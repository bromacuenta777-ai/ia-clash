package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeckArchetype
import com.example.model.GeminiStrategicReport
import com.example.model.TacticalMove
import com.example.model.TelemetryLogEntry
import com.example.model.VisionFrameState
import com.example.ui.SystemReadinessState
import com.example.ui.components.ElixirTelemetryBar
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.CrimsonThreat
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun CommandScreen(
    readiness: SystemReadinessState,
    accessibilityConnected: Boolean,
    captureRunning: Boolean,
    botPlaying: Boolean,
    overlayVisible: Boolean,
    selectedDeck: DeckArchetype,
    visionState: VisionFrameState,
    lastMove: TacticalMove?,
    lastGeminiReport: GeminiStrategicReport?,
    gesturesExecuted: Int,
    logs: List<TelemetryLogEntry>,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onStopScreenCapture: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onToggleBotPlaying: () -> Unit,
    onLaunchClashRoyale: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            HeroCommandCard(
                accessibilityConnected = accessibilityConnected,
                captureRunning = captureRunning,
                botPlaying = botPlaying,
                selectedDeck = selectedDeck,
                gesturesExecuted = gesturesExecuted,
                clashInstalled = readiness.clashRoyaleInstalled,
                onRequestScreenCapture = onRequestScreenCapture,
                onStopScreenCapture = onStopScreenCapture,
                onToggleBotPlaying = onToggleBotPlaying,
                onLaunchClashRoyale = onLaunchClashRoyale
            )
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
                    Text(
                        text = "REQUISITOS EN EL TELÉFONO (SIN PC)",
                        style = MaterialTheme.typography.titleMedium,
                        color = RoyalGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Activa estos permisos nativos para que la IA vea la arena y toque cartas automáticamente sobre Clash Royale.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    RequirementRow(
                        icon = Icons.Filled.AccessibilityNew,
                        title = "1. Control Táctil (Accesibilidad)",
                        subtitle = if (accessibilityConnected) "Conectado y listo para ejecutar gestos"
                        else "Activa 'RoyaleAI Control Táctil' en Ajustes",
                        isReady = accessibilityConnected,
                        actionLabel = if (accessibilityConnected) "Configurar" else "Activar",
                        testTag = "accessibility_setup_button",
                        onClick = onOpenAccessibilitySettings
                    )

                    RequirementRow(
                        icon = Icons.Filled.ScreenShare,
                        title = "2. Visión de Pantalla en Vivo",
                        subtitle = if (captureRunning) "MediaProjection activo leyendo elixir y amenazas"
                        else "Captura frames de la arena directamente en tu móvil",
                        isReady = captureRunning,
                        actionLabel = if (captureRunning) "Detener" else "Permitir",
                        testTag = "screen_capture_button",
                        onClick = if (captureRunning) onStopScreenCapture else onRequestScreenCapture
                    )

                    RequirementRow(
                        icon = Icons.Filled.NotificationsActive,
                        title = "3. Servicio en Segundo Plano",
                        subtitle = if (readiness.notificationGranted) "Notificación persistente autorizada"
                        else "Permite notificaciones para mantener la IA activa",
                        isReady = readiness.notificationGranted,
                        actionLabel = if (readiness.notificationGranted) "Listo" else "Permitir",
                        testTag = "notification_permission_button",
                        onClick = onRequestNotificationPermission
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianCardElevated)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = "HUD Flotante",
                                tint = ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "HUD Flotante sobre Clash Royale",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Muestra Play/Pausa y Elixir encima del juego",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = overlayVisible,
                            onCheckedChange = onToggleOverlay,
                            modifier = Modifier.testTag("overlay_toggle_switch")
                        )
                    }
                }
            }
        }

        item {
            LiveTelemetryCard(
                visionState = visionState,
                lastMove = lastMove,
                lastGeminiReport = lastGeminiReport
            )
        }

        item {
            Text(
                text = "REGISTRO DE DECISIONES EN VIVO",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 4.dp)
            )
        }

        items(logs.take(15), key = { it.id }) { entry ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (entry.isHighlight) ObsidianCardElevated else ObsidianCard,
                border = BorderStroke(
                    1.dp,
                    if (entry.isHighlight) ElectricCyan.copy(alpha = 0.4f) else ObsidianBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricCyan.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = entry.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCommandCard(
    accessibilityConnected: Boolean,
    captureRunning: Boolean,
    botPlaying: Boolean,
    selectedDeck: DeckArchetype,
    gesturesExecuted: Int,
    clashInstalled: Boolean,
    onRequestScreenCapture: () -> Unit,
    onStopScreenCapture: () -> Unit,
    onToggleBotPlaying: () -> Unit,
    onLaunchClashRoyale: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.5.dp, if (botPlaying) EmeraldReady else ObsidianBorder)
    ) {
        Box {
            Image(
                painter = painterResource(id = R.drawable.img_arena_hero),
                contentDescription = "Arena táctica RoyaleAI",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianDeep.copy(alpha = 0.35f),
                                ObsidianCard.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPillBadge(
                        label = when {
                            botPlaying -> "IA JUGANDO EN VIVO"
                            captureRunning -> "VISIÓN ACTIVA"
                            else -> "EN ESPERA"
                        },
                        isActive = botPlaying || captureRunning,
                        activeColor = if (botPlaying) EmeraldReady else ElectricCyan
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianDeep.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Jugadas: $gesturesExecuted",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column {
                    Text(
                        text = "Agente Top-1 Autónomo",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mazo: ${selectedDeck.displayName} (${selectedDeck.avgElixir} elixir)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoyalGold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!captureRunning) {
                        Button(
                            onClick = onRequestScreenCapture,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("start_vision_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Iniciar Visión + HUD",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Button(
                            onClick = onToggleBotPlaying,
                            enabled = accessibilityConnected,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("toggle_bot_play_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (botPlaying) RoyalGold else EmeraldReady,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Icon(
                                imageVector = if (botPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (botPlaying) "Pausar IA" else "Activar IA",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onStopScreenCapture,
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("stop_vision_button"),
                            border = BorderStroke(1.dp, CrimsonThreat.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonThreat)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Detener")
                        }
                    }
                }

                OutlinedButton(
                    onClick = onLaunchClashRoyale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("launch_clash_royale_button"),
                    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (clashInstalled) "Abrir Clash Royale y Empezar"
                        else "Abrir Clash Royale (Instalar desde Play Store)"
                    )
                }
            }
        }
    }
}

@Composable
private fun RequirementRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isReady: Boolean,
    actionLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCardElevated)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isReady) EmeraldReady else RoyalGold,
            modifier = Modifier.size(24.dp)
        )
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
        Button(
            onClick = onClick,
            modifier = Modifier
                .height(44.dp)
                .testTag(testTag),
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isReady) EmeraldReady.copy(alpha = 0.18f) else ElectricCyan,
                contentColor = if (isReady) EmeraldReady else ObsidianDeep
            )
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveTelemetryCard(
    visionState: VisionFrameState,
    lastMove: TacticalMove?,
    lastGeminiReport: GeminiStrategicReport?
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TELEMETRÍA DE ARENA",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
                StatusPillBadge(
                    label = visionState.battlePhase.labelEs,
                    isActive = visionState.elixir > 0f,
                    activeColor = ElectricCyan
                )
            }

            ElixirTelemetryBar(elixir = visionState.elixir)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Amenaza Izquierda: ${(visionState.laneThreat.leftThreat * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { visionState.laneThreat.leftThreat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CrimsonThreat,
                        trackColor = ObsidianDeep
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Amenaza Derecha: ${(visionState.laneThreat.rightThreat * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { visionState.laneThreat.rightThreat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CrimsonThreat,
                        trackColor = ObsidianDeep
                    )
                }
            }

            if (lastMove != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCardElevated,
                    border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ÚLTIMA JUGADA EJECUTADA",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldReady
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${lastMove.cardName} -> ${lastMove.tileLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = lastMove.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (lastGeminiReport != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricCyan.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ESTRATEGIA GEMINI (${lastGeminiReport.enemyArchetype})",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastGeminiReport.macroAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
