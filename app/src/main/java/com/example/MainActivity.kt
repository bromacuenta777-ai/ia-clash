package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.engine.configureAppCheck
import com.example.service.ScreenCaptureService
import com.example.ui.RoyaleViewModel
import com.example.ui.components.StatusPillBadge
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.CommandScreen
import com.example.ui.screens.LabScreen
import com.example.ui.screens.StrategyScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyaleAITheme
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

const val ROUTE_COMMAND = "command"
const val ROUTE_STRATEGY = "strategy"
const val ROUTE_CALIBRATION = "calibration"
const val ROUTE_LAB = "lab"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navTabs = listOf(
    NavTab(
        route = ROUTE_COMMAND,
        label = "Comando",
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports,
        testTag = "nav_command"
    ),
    NavTab(
        route = ROUTE_STRATEGY,
        label = "Cerebro",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
        testTag = "nav_strategy"
    ),
    NavTab(
        route = ROUTE_CALIBRATION,
        label = "Calibrar",
        selectedIcon = Icons.Filled.GridOn,
        unselectedIcon = Icons.Outlined.GridOn,
        testTag = "nav_calibration"
    ),
    NavTab(
        route = ROUTE_LAB,
        label = "Lab IA",
        selectedIcon = Icons.Filled.Science,
        unselectedIcon = Icons.Outlined.Science,
        testTag = "nav_lab"
    )
)

class MainActivity : ComponentActivity() {

    private val viewModel: RoyaleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // App Check MUST be configured with launch intent before UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            RoyaleAITheme {
                RoyaleAIAppShell(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemReadiness()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoyaleAIAppShell(viewModel: RoyaleViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_COMMAND

    val readiness by viewModel.systemReadiness.collectAsStateWithLifecycle()
    val accessibilityConnected by viewModel.accessibilityConnected.collectAsStateWithLifecycle()
    val captureRunning by viewModel.captureRunning.collectAsStateWithLifecycle()
    val botPlaying by viewModel.botPlaying.collectAsStateWithLifecycle()
    val overlayVisible by viewModel.overlayVisible.collectAsStateWithLifecycle()
    val selectedDeck by viewModel.selectedDeck.collectAsStateWithLifecycle()
    val gestureMode by viewModel.gestureMode.collectAsStateWithLifecycle()
    val aggressionProfile by viewModel.aggressionProfile.collectAsStateWithLifecycle()
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val visionState by viewModel.visionState.collectAsStateWithLifecycle()
    val lastMove by viewModel.lastMove.collectAsStateWithLifecycle()
    val lastGeminiReport by viewModel.lastGeminiReport.collectAsStateWithLifecycle()
    val gesturesExecuted by viewModel.gesturesExecuted.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val simulatorState by viewModel.simulatorState.collectAsStateWithLifecycle()
    val geminiUiState by viewModel.geminiUiState.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.selectedPreviewBitmap.collectAsStateWithLifecycle()
    val totalGeminiScans by viewModel.totalGeminiScans.collectAsStateWithLifecycle()

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val startIntent = ScreenCaptureService.createStartIntent(
                context = context,
                resultCode = result.resultCode,
                data = data
            )
            ContextCompat.startForegroundService(context, startIntent)
            scope.launch {
                snackbarHostState.showSnackbar("Visión en vivo activada. Abre Clash Royale y pulsa INICIAR IA en el HUD.")
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Se canceló el permiso de captura de pantalla.")
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshSystemReadiness()
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Notificaciones de servicio autorizadas."
                else "Permiso de notificaciones denegado; puedes activarlo en Ajustes."
            )
        }
    }

    val requestScreenCapture: () -> Unit = {
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Este Android no requiere permiso manual de notificaciones.")
            }
        }
    }

    val openAccessibilitySettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        scope.launch {
            snackbarHostState.showSnackbar("Activa 'RoyaleAI Control Táctil' en Servicios instalados.")
        }
    }

    val launchClashOrStore: () -> Unit = {
        val launched = viewModel.launchClashRoyale()
        if (!launched) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.supercell.clashroyale")
                    )
                )
            } catch (_: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Instala Clash Royale en este dispositivo para jugar en vivo.")
                }
            }
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianDeep,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row {
                            Text(
                                text = "ROYALE",
                                style = MaterialTheme.typography.titleLarge,
                                color = RoyalGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        StatusPillBadge(
                            label = if (accessibilityConnected) "TÁCTIL OK" else "SIN TÁCTIL",
                            isActive = accessibilityConnected,
                            activeColor = EmeraldReady,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ObsidianCard,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ObsidianCard,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_COMMAND
                    ) {
                        composable(ROUTE_COMMAND) {
                            CommandScreen(
                                readiness = readiness,
                                accessibilityConnected = accessibilityConnected,
                                captureRunning = captureRunning,
                                botPlaying = botPlaying,
                                overlayVisible = overlayVisible,
                                selectedDeck = selectedDeck,
                                visionState = visionState,
                                lastMove = lastMove,
                                lastGeminiReport = lastGeminiReport,
                                gesturesExecuted = gesturesExecuted,
                                logs = logs,
                                onOpenAccessibilitySettings = openAccessibilitySettings,
                                onRequestScreenCapture = requestScreenCapture,
                                onStopScreenCapture = { viewModel.stopCaptureService() },
                                onRequestNotificationPermission = requestNotificationPermission,
                                onToggleOverlay = { viewModel.setOverlayVisible(it) },
                                onToggleBotPlaying = { viewModel.toggleBotPlaying() },
                                onLaunchClashRoyale = launchClashOrStore
                            )
                        }

                        composable(ROUTE_STRATEGY) {
                            StrategyScreen(
                                selectedDeck = selectedDeck,
                                gestureMode = gestureMode,
                                aggressionProfile = aggressionProfile,
                                calibration = calibration,
                                onSelectDeck = { viewModel.selectDeck(it) },
                                onSelectGestureMode = { viewModel.selectGestureMode(it) },
                                onSelectAggression = { viewModel.selectAggression(it) },
                                onUpdateCalibration = { viewModel.updateCalibration(it) }
                            )
                        }

                        composable(ROUTE_CALIBRATION) {
                            CalibrationScreen(
                                calibration = calibration,
                                accessibilityConnected = accessibilityConnected,
                                onApplyPreset = { viewModel.applyCalibrationPreset(it) },
                                onUpdateCalibration = { viewModel.updateCalibration(it) },
                                onTestTap = { viewModel.testAccessibilityTapOnCenter() }
                            )
                        }

                        composable(ROUTE_LAB) {
                            LabScreen(
                                selectedDeck = selectedDeck,
                                simulatorState = simulatorState,
                                geminiUiState = geminiUiState,
                                previewBitmap = previewBitmap,
                                totalGeminiScans = totalGeminiScans,
                                onRunSimulatorScenario = { name, col, row, label ->
                                    viewModel.runSimulatorScenario(name, col, row, label)
                                },
                                onAnalyzePhotoUri = { uri ->
                                    viewModel.analyzePhotoUriWithGemini(uri)
                                },
                                onAnalyzeLiveFrame = {
                                    viewModel.analyzeLiveFrameWithGemini()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
