package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.SiteTracerTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SiteTracerTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }

                // Request Permissions on Startup
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                Scaffold(
                    topBar = {
                        if (currentTab == NavTab.MAP) {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = "Cell Towers",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { currentTab = NavTab.DASHBOARD }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back to Dashboard",
                                            tint = Color.White
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { viewModel.announceCurrentTelemetry() }) {
                                        Icon(
                                            imageVector = Icons.Default.FormatListBulleted,
                                            contentDescription = "Sites List",
                                            tint = Color.White
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color(0xFF0D1B2A)
                                )
                            )
                        } else {
                            CenterAlignedTopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = "SITE & SECTOR TRACER",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${uiState.cellInfo.operatorName} • Band ${uiState.cellInfo.frequencyLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { viewModel.announceCurrentTelemetry() }) {
                                        Icon(
                                            imageVector = if (uiState.isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                            contentDescription = "Announce Telemetry Voice",
                                            tint = if (uiState.isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NavTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title) }
                                )
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets.safeDrawing,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            NavTab.DASHBOARD -> DashboardScreen(
                                uiState = uiState,
                                onToggleVoiceAlerts = { viewModel.toggleVoiceAlerts(it) },
                                onTestVoiceAlert = { viewModel.announceCurrentTelemetry() },
                                onToggleAutoRefresh = { viewModel.setAutoRefreshMode(it) },
                                onRefreshNetwork = { viewModel.refreshNetworkData() },
                                onSelectBand = { earfcn ->
                                    viewModel.toggleSimulationMode(true)
                                    viewModel.setSimulationBand(earfcn)
                                }
                            )
                            NavTab.MAP -> MapScreen(
                                uiState = uiState,
                                onSpeakTest = { viewModel.announceCurrentTelemetry() }
                            )
                            NavTab.SPEED -> SpeedTestScreen(
                                uiState = uiState,
                                onSpeakResults = { message -> viewModel.speakCustomText(message) },
                                onToggleVoiceAlerts = { viewModel.toggleVoiceAlerts(it) }
                            )
                            NavTab.KMZ_SITES -> SitesManagerScreen(
                                uiState = uiState,
                                onImportKmzFile = { uri -> viewModel.importKmzFile(uri) },
                                onLoadSampleKmz = { viewModel.loadSampleKmzData() },
                                onClearSites = { viewModel.clearAllSites() }
                            )
                            NavTab.HISTORY -> LogHistoryScreen(
                                uiState = uiState,
                                onClearLogs = { viewModel.clearLogs() }
                            )
                            NavTab.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onToggleVoiceAlerts = { viewModel.toggleVoiceAlerts(it) },
                                onTestVoiceAlert = { viewModel.speakTestAlert() },
                                onToggleSimulationMode = { viewModel.toggleSimulationMode(it) },
                                onSelectSimulationBand = { earfcn, site, sec ->
                                    viewModel.setSimulationBand(earfcn, site, sec)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class NavTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Tracer", Icons.Default.SignalCellular4Bar),
    MAP("Map", Icons.Default.Map),
    SPEED("Speed", Icons.Default.Speed),
    KMZ_SITES("Sites", Icons.Default.CellTower),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings)
}
