package ar.motorfar.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Observer
import ar.motorfar.app.data.AppSetting
import ar.motorfar.app.data.ChannelMemory
import ar.motorfar.app.radio.RadioAudioService
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ar.motorfar.app.R
import ar.motorfar.app.ui.compose.AliasSettingScreen
import ar.motorfar.app.ui.compose.ChatScreen
import ar.motorfar.app.ui.compose.MainScreen
import ar.motorfar.app.ui.compose.MapScreen
import ar.motorfar.app.ui.compose.state.GroupMember
import ar.motorfar.app.ui.compose.state.ChatMessage
import ar.motorfar.app.ui.compose.state.MainUiAction
import ar.motorfar.app.ui.compose.state.MainUiState
import ar.motorfar.app.ui.compose.theme.AppTheme
import ar.motorfar.app.ui.compose.theme.EmergencyText
import ar.motorfar.app.ui.compose.theme.LocalMotoRFARColors
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme
import ar.motorfar.app.ui.compose.GpsBeaconManager
import ar.motorfar.app.ui.compose.MovementState
import ar.motorfar.app.aprs.parser.APRSPacket
import ar.motorfar.app.aprs.parser.APRSTypes
import ar.motorfar.app.aprs.parser.MessagePacket
import ar.motorfar.app.aprs.parser.PositionField
import ar.motorfar.app.ui.compose.state.ReceivedAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ar.motorfar.app.ui.OnboardingHelper
import ar.motorfar.app.ui.onboarding.OnboardingActivity
import android.view.WindowManager
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val _uiState = MutableStateFlow(MainUiState())
    private val uiState = _uiState.asStateFlow()

    private var radioService: RadioAudioService? = null
    private var radioServiceBound = false
    private var callsign: String = ""
    private var activeMemoryId: Int = -1
    private var activeFrequencyStr: String = "139.9700"
    private var userAlias: String by mutableStateOf("MOTO")
    private var beaconIntervalSec: Int = 60
    private var alertVolume: Int = 70
    private var listenOnly: Boolean = false
    private var smartBeaconEnabled: Boolean = true

    // Estado de movimiento del GPS (alimenta SmartBeaconing y Modo Ruta)
    @Volatile private var currentSpeedKmh: Double = 0.0
    @Volatile private var currentHeadingDeg: Double? = null
    private var stationaryStartMs: Long = 0L
    private val routeAutoOffHandler = Handler(Looper.getMainLooper())
    private val routeAutoOffRunnable = Runnable {
        // Auto-desactiva Modo Ruta si lleva >60 s detenido (<3 km/h)
        if (uiState.value.isRouteActive && currentSpeedKmh < 3.0) {
            _uiState.update { it.copy(isRouteActive = false) }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            android.widget.Toast.makeText(
                this, "MODO RUTA · desactivado (vehículo detenido)", android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var chatMessageIdCounter = 0L

    // Punto al que centrar el mapa al tocar "ir a ubicación" en una alerta del chat
    private val _mapFocus = MutableStateFlow<Pair<Double, Double>?>(null)

    private var pendingAlertType: AlertHelper.AlertType? = null

    private val _beaconIntervalFlow = MutableStateFlow(60_000L)

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val beaconManager by lazy {
        GpsBeaconManager(
            // En modo escucha NO se emiten beacons de posición (RX-only real)
            onSendBeacon         = { if (!listenOnly) radioService?.sendPositionBeacon() },
            intervalFlow         = _beaconIntervalFlow,
            smartEnabledProvider = { smartBeaconEnabled && !listenOnly },
            movementProvider     = { MovementState(currentSpeedKmh, currentHeadingDeg) }
        )
    }

    // Listener de GPS para alimentar SmartBeaconing con velocidad y rumbo reales
    private val movementListener = android.location.LocationListener { loc ->
        // speed viene en m/s → km/h
        currentSpeedKmh = (loc.speed * 3.6).toDouble()
        if (loc.hasBearing() && loc.speed > 0.5f) {
            currentHeadingDeg = loc.bearing.toDouble()
            _uiState.update { it.copy(headingDeg = loc.bearing) }
        }
        // Modo Ruta: si la velocidad baja de 3 km/h, programa la auto-desactivación
        if (uiState.value.isRouteActive) {
            if (currentSpeedKmh < 3.0) {
                if (stationaryStartMs == 0L) {
                    stationaryStartMs = System.currentTimeMillis()
                    routeAutoOffHandler.postDelayed(routeAutoOffRunnable, 60_000L)
                }
            } else {
                stationaryStartMs = 0L
                routeAutoOffHandler.removeCallbacks(routeAutoOffRunnable)
            }
        }
    }

    private val executor = Executors.newSingleThreadExecutor()

    // ── Permissions ──────────────────────────────────────────────────
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingAlertType?.let { transmitGroupAlert(it) }
        pendingAlertType = null
    }

    // Pedido de ubicación para el mapa (solo refresca la pantalla, sin TX)
    private val mapLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        _uiState.update { it.copy(locationGranted = granted) }
    }

    // ── ServiceConnection ─────────────────────────────────────────────
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            radioService = (binder as RadioAudioService.RadioBinder).service
            radioServiceBound = true
            radioService?.let { RadioServiceAccessor.setCallbacks(it, serviceCallbacks) }
            _uiState.update { it.copy(isConnected = true) }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            radioService = null
            radioServiceBound = false
            _uiState.update { it.copy(isConnected = false) }
        }
    }

    // ── RadioAudioService Callbacks ───────────────────────────────────
    private val serviceCallbacks = object : RadioAudioService.RadioAudioServiceCallbacks {
        override fun radioConnected() {
            _uiState.update { it.copy(isConnected = true) }
        }
        override fun radioMissing() {
            _uiState.update { it.copy(isConnected = false, sMeterLevel = 0) }
        }
        override fun sMeterUpdate(value: Int) {
            _uiState.update { it.copy(sMeterLevel = value) }
        }
        override fun txStarted() {
            _uiState.update { it.copy(isTxActive = true) }
        }
        override fun txEnded() {
            _uiState.update { it.copy(isTxActive = false) }
        }
        override fun moduleTxStateChanged(txActive: Boolean) {
            if (!txActive) {
                // Fin de TX confirmado por el módulo → roger beep estilo VHF real
                ToneHelper.playRogerBeep(alertVolume / 100f)
            }
            _uiState.update { it.copy(isTxActive = txActive) }
        }
        override fun tunedToFreq(frequencyStr: String) {
            _uiState.update { it.copy(activeFrequency = frequencyStr) }
            activeFrequencyStr = frequencyStr
        }
        override fun packetReceived(aprsPacket: APRSPacket) {
            val infoField = RadioServiceAccessor.getAprsPayload(aprsPacket) ?: return
            val alias = RadioServiceAccessor.getAprsSourceCall(aprsPacket)?.trim() ?: return

            val posField = infoField.getAprsData(APRSTypes.T_POSITION) as? PositionField
            if (posField != null) {
                val member = GroupMember(
                    alias      = alias,
                    lat        = posField.position.latitude,
                    lon        = posField.position.longitude,
                    distanceM  = 0,
                    bearing    = 0f,
                    lastSeenMs = System.currentTimeMillis()
                )
                _groupMembers.update { list -> list.filter { it.alias != alias } + member }
            }

            val msgPacket = infoField as? MessagePacket
            if (msgPacket != null) {
                val body = msgPacket.messageBody ?: return
                val alertType = when {
                    body.contains("ALERTA")        -> AlertHelper.AlertType.EMERGENCY
                    body.contains("DETENCION")     -> AlertHelper.AlertType.STOP
                    body.contains("REAGRUPAMIENTO") -> AlertHelper.AlertType.REGROUP
                    else                           -> null
                }
                if (alertType != null) {
                    val alert = ReceivedAlert(
                        type          = alertType,
                        fromAlias     = alias,
                        receivedAtMs  = System.currentTimeMillis()
                    )
                    _uiState.update { s ->
                        s.copy(
                            activeAlert  = alert,
                            alertHistory = (listOf(alert) + s.alertHistory).take(5)
                        )
                    }
                    // También al chat con formato de alerta (posición si vino en el mismo beacon)
                    val pos = infoField.getAprsData(APRSTypes.T_POSITION) as? PositionField
                    addAlertToChat(
                        alertType, alias, outgoing = false,
                        lat = pos?.position?.latitude,
                        lon = pos?.position?.longitude
                    )
                    ToneHelper.playAlertBeep(alertVolume / 100f)
                    Handler(Looper.getMainLooper()).postDelayed({
                        _uiState.update { if (it.activeAlert?.fromAlias == alias) it.copy(activeAlert = null) else it }
                    }, 30_000L)
                } else {
                    // Mensaje de chat normal (no es alerta) → al chat VHF
                    addChatMessage(alias, body, outgoing = false)
                    ToneHelper.playAlertBeep(alertVolume / 100f)
                }
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Aplica el tema guardado ANTES de componer (evita el flash del default)
        _uiState.update { it.copy(theme = ar.motorfar.app.ui.compose.theme.ThemePreference.get(this)) }

        val onboardingExecutor = Executors.newSingleThreadExecutor()
        onboardingExecutor.execute {
            val settings = RadioServiceAccessor.getAppDb(viewModel).appSettingDao().getAll()
                .associateBy(AppSetting::name, AppSetting::value)
            if (OnboardingHelper.shouldShowOnboarding(settings)) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        stopService(Intent(this, RadioAudioService::class.java))
                        startActivity(Intent(this, OnboardingActivity::class.java))
                        finish()
                    }
                }
            }
            onboardingExecutor.shutdown()
        }

        viewModel.loadDataAsync { loadSettings() }

        observeChannelMemories()

        // DEMO: miembros de grupo simulados para visualizar el mapa sin hardware.
        // (solo en debug — quitar/condicionar para producción)
        if (ar.motorfar.app.BuildConfig.DEBUG) {
            seedDemoGroupMembers()
        }

        setContent {
            val state by uiState.collectAsState()
            val groupMembers by _groupMembers.collectAsState()
            val chatMessages by _chatMessages.collectAsState()
            val mapFocus by _mapFocus.collectAsState()
            MotoRFARTheme(state.theme) {
                val colors = LocalMotoRFARColors.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "main"
                val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                Scaffold(
                    containerColor = colors.background,
                    bottomBar = {
                        // En horizontal la navegación va en un NavigationRail lateral (libera alto)
                        if (!isLandscape) {
                        NavigationBar(containerColor = colors.surface) {
                            if (currentRoute != "main") {
                                NavigationBarItem(
                                    selected = false,
                                    onClick  = { navController.navigate("main") { launchSingleTop = true } },
                                    icon     = { Icon(painterResource(R.drawable.ic_mic), contentDescription = "PTT") },
                                    label    = { androidx.compose.material3.Text("PTT", color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono) }
                                )
                            }
                            if (currentRoute != "map") {
                                NavigationBarItem(
                                    selected = false,
                                    onClick  = { navController.navigate("map") { launchSingleTop = true } },
                                    icon     = { Icon(painterResource(R.drawable.ic_pin), contentDescription = "MAPA") },
                                    label    = { androidx.compose.material3.Text("MAPA", color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono) }
                                )
                            }
                            if (currentRoute != "chat") {
                                NavigationBarItem(
                                    selected = false,
                                    onClick  = { navController.navigate("chat") { launchSingleTop = true } },
                                    icon     = { Icon(painterResource(R.drawable.ic_text_chat_mode), contentDescription = "CHAT") },
                                    label    = { androidx.compose.material3.Text("CHAT", color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono) }
                                )
                            }
                        }
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .padding(innerPadding)
                            // Mantiene el rail y las pantallas fuera del recorte de
                            // la cámara (isla) en horizontal — antes lo pisaba.
                            .displayCutoutPadding()
                            .fillMaxSize()
                    ) {
                    if (isLandscape) {
                        NavigationRail(containerColor = colors.surface) {
                            if (currentRoute != "main") {
                                NavigationRailItem(
                                    selected = false,
                                    onClick  = { navController.navigate("main") { launchSingleTop = true } },
                                    icon     = { Icon(painterResource(R.drawable.ic_mic), contentDescription = "PTT") },
                                    label    = { androidx.compose.material3.Text("PTT", color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono, fontSize = 20.sp) }
                                )
                            }
                            if (currentRoute != "map") {
                                NavigationRailItem(
                                    selected = false,
                                    onClick  = { navController.navigate("map") { launchSingleTop = true } },
                                    icon     = { Icon(painterResource(R.drawable.ic_pin), contentDescription = "MAPA") },
                                    label    = { androidx.compose.material3.Text("MAPA", color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono, fontSize = 20.sp) }
                                )
                            }
                            if (currentRoute != "chat") {
                                NavigationRailItem(
                                    selected = false,
                                    onClick  = { navController.navigate("chat") { launchSingleTop = true } },
                                    icon     = { Icon(painterResource(R.drawable.ic_text_chat_mode), contentDescription = "CHAT") },
                                    label    = { androidx.compose.material3.Text("CHAT", color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono, fontSize = 20.sp) }
                                )
                            }
                        }
                    }
                    NavHost(
                        navController  = navController,
                        startDestination = "main",
                        modifier       = Modifier.weight(1f).fillMaxSize()
                    ) {
                        composable("main") {
                            MainScreen(
                                state          = state,
                                onAction       = ::handleAction,
                                onDismissAlert = { _uiState.update { it.copy(activeAlert = null) } },
                                onOpenSettings = { navController.navigate("settings") { launchSingleTop = true } }
                            )
                        }
                        composable("map") {
                            // Al entrar al mapa, asegura el permiso de ubicación
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                    this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    _uiState.update { it.copy(locationGranted = true) }
                                } else {
                                    mapLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            }
                            MapScreen(
                                groupMembers    = groupMembers,
                                locationGranted = state.locationGranted,
                                headingDeg      = state.headingDeg,
                                focusTarget     = mapFocus,
                                onFocusConsumed = { _mapFocus.value = null },
                                isTransmitting  = state.isTxActive,
                                listenOnly      = state.isListenOnly,
                                onPttDown       = { handleAction(MainUiAction.PttPressed) },
                                onPttUp         = { handleAction(MainUiAction.PttReleased) }
                            )
                        }
                        composable("settings") {
                            AliasSettingScreen(
                                currentAlias             = userAlias,
                                currentBeaconIntervalSec = beaconIntervalSec,
                                currentVolume            = alertVolume,
                                currentSmartBeacon       = smartBeaconEnabled,
                                onSave                   = ::saveAliasSettings,
                                onToggleSmartBeacon      = { enabled ->
                                    smartBeaconEnabled = enabled
                                    // Reinicia el balizado con el nuevo modo
                                    _beaconIntervalFlow.value = beaconIntervalSec * 1000L
                                    executor.execute {
                                        RadioServiceAccessor.getAppDb(viewModel)
                                            .saveAppSetting(AppSetting.SETTING_SMART_BEACON, enabled.toString())
                                    }
                                },
                                onDownloadMaps           = {
                                    android.widget.Toast.makeText(
                                        this@MainActivity,
                                        "Descarga de mapas offline — próximamente",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onPrivacyPolicy          = {
                                    startActivity(android.content.Intent(this@MainActivity, PrivacyPolicyActivity::class.java))
                                },
                                onAbout                  = {
                                    startActivity(android.content.Intent(this@MainActivity, AboutActivity::class.java))
                                }
                            )
                        }
                        composable("chat") {
                            ChatScreen(
                                messages       = chatMessages,
                                alertHistory   = state.alertHistory,
                                onSend         = ::sendChatMessage,
                                onGoToLocation = { lat, lon ->
                                    _mapFocus.value = lat to lon
                                    navController.navigate("map") { launchSingleTop = true }
                                }
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startAndBindService()
        beaconManager.start(serviceScope)
        startMovementUpdates()
    }

    override fun onStop() {
        super.onStop()
        beaconManager.stop()
        stopMovementUpdates()
        if (radioServiceBound) {
            unbindService(serviceConnection)
            radioServiceBound = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMovementUpdates() {
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            // Actualizaciones cada 1s o 2m para tener velocidad/rumbo frescos
            lm.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER,
                1000L, 2f, movementListener
            )
        } catch (e: Exception) { /* GPS no disponible */ }
    }

    private fun stopMovementUpdates() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            lm.removeUpdates(movementListener)
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        executor.shutdownNow()
        routeAutoOffHandler.removeCallbacks(routeAutoOffRunnable)
    }

    // ── Service binding ───────────────────────────────────────────────
    private fun startAndBindService() {
        val effectiveCallsign = userAlias.ifBlank { callsign }
        val intent = Intent(this, RadioAudioService::class.java)
            .putExtra(AppSetting.SETTING_CALLSIGN, effectiveCallsign)
            .putExtra("activeMemoryId", activeMemoryId)
            .putExtra("activeFrequencyStr", activeFrequencyStr)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun loadSettings() {
        val settings = RadioServiceAccessor.getAppDb(viewModel).appSettingDao().getAll()
            .associateBy(AppSetting::name, AppSetting::value)
        callsign           = settings.getOrDefault(AppSetting.SETTING_CALLSIGN, "")
        activeFrequencyStr = settings.getOrDefault("activeFrequencyStr", "139.9700")
        userAlias          = settings.getOrDefault(AppSetting.SETTING_USER_ALIAS, "MOTO")
        beaconIntervalSec  = settings.getOrDefault(AppSetting.SETTING_BEACON_INTERVAL_SEC, "60").toIntOrNull() ?: 60
        alertVolume        = settings.getOrDefault(AppSetting.SETTING_ALERT_VOLUME, "70").toIntOrNull() ?: 70
        listenOnly         = settings.getOrDefault(AppSetting.SETTING_LISTEN_ONLY, "false").toBoolean()
        smartBeaconEnabled = settings.getOrDefault(AppSetting.SETTING_SMART_BEACON, "true").toBoolean()
        _beaconIntervalFlow.value = beaconIntervalSec * 1000L
        runOnUiThread {
            _uiState.update { it.copy(activeFrequency = activeFrequencyStr, isListenOnly = listenOnly) }
        }
    }

    private fun saveAliasSettings(alias: String, intervalSec: Int, volume: Int) {
        userAlias                 = alias
        beaconIntervalSec         = intervalSec
        alertVolume               = volume
        _beaconIntervalFlow.value = intervalSec * 1000L
        executor.execute {
            val db = RadioServiceAccessor.getAppDb(viewModel)
            db.saveAppSetting(AppSetting.SETTING_USER_ALIAS, alias)
            db.saveAppSetting(AppSetting.SETTING_BEACON_INTERVAL_SEC, intervalSec.toString())
            db.saveAppSetting(AppSetting.SETTING_ALERT_VOLUME, volume.toString())
        }
    }

    private fun observeChannelMemories() {
        viewModel.channelMemories.observe(this, Observer { memories ->
            if (memories == null) return@Observer
            val active = memories.firstOrNull { it.frequency == activeFrequencyStr }
            _uiState.update { s ->
                s.copy(
                    channels          = memories,
                    activeChannelName = active?.name ?: "SIMPLEX"
                )
            }
            radioService?.setChannelMemories(viewModel.channelMemories)
        })
    }

    // ── Action handler ────────────────────────────────────────────────
    private fun handleAction(action: MainUiAction) {
        val vol = alertVolume / 100f
        when (action) {
            MainUiAction.PttPressed    -> {
                // En modo escucha el PTT no transmite (guard de seguridad)
                if (listenOnly) { ToneHelper.playAlertBeep(vol); notifyListenOnlyBlocked(); return }
                ToneHelper.playPttDown(vol)
                val svc = radioService
                if (svc != null && uiState.value.isConnected) {
                    svc.startPtt()
                } else {
                    // Modo simulación: sin radio, solo feedback visual (anillos TX)
                    _uiState.update { it.copy(isTxActive = true) }
                }
            }
            MainUiAction.PttReleased   -> {
                if (listenOnly) return
                val svc = radioService
                if (svc != null && uiState.value.isConnected) {
                    // El roger beep lo dispara moduleTxStateChanged(false) cuando el
                    // módulo confirma el fin de TX (evita el doble sonido al soltar).
                    svc.endPtt()
                } else {
                    // Fin de simulación: sin módulo, el roger beep va acá
                    _uiState.update { it.copy(isTxActive = false) }
                    ToneHelper.playRogerBeep(vol)
                }
            }
            is MainUiAction.ChannelSelected -> tuneToChannel(action.freq)
            // EMERGENCIA siempre disponible, incluso en modo escucha (seguridad)
            MainUiAction.EmergencyAlert -> { ToneHelper.playEmergencyBeep(vol); requestLocationAndTransmit(AlertHelper.AlertType.EMERGENCY) }
            MainUiAction.StopAlert     -> {
                if (listenOnly) { ToneHelper.playAlertBeep(vol); notifyListenOnlyBlocked(); return }
                ToneHelper.playAlertBeep(vol); showAlertDialog(AlertHelper.AlertType.STOP)
            }
            MainUiAction.RegroupAlert  -> {
                if (listenOnly) { ToneHelper.playAlertBeep(vol); notifyListenOnlyBlocked(); return }
                ToneHelper.playAlertBeep(vol); showAlertDialog(AlertHelper.AlertType.REGROUP)
            }
            MainUiAction.ToggleListenOnly -> toggleListenOnly()
            is MainUiAction.SetTheme -> {
                ar.motorfar.app.ui.compose.theme.ThemePreference.set(this, action.theme)
                _uiState.update { it.copy(theme = action.theme) }
                ToneHelper.playPttUp(vol)
            }
            MainUiAction.ToggleRouteActive -> {
                val nowActive = !uiState.value.isRouteActive
                _uiState.update { it.copy(isRouteActive = nowActive) }
                if (nowActive) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    stationaryStartMs = 0L
                    android.widget.Toast.makeText(
                        this, "MODO RUTA ACTIVO · pantalla siempre encendida", android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    stationaryStartMs = 0L
                    routeAutoOffHandler.removeCallbacks(routeAutoOffRunnable)
                }
                ToneHelper.playPttUp(vol)
            }
            MainUiAction.SendWaypoint -> {
                if (listenOnly) { ToneHelper.playAlertBeep(vol); notifyListenOnlyBlocked(); return }
                val svc = radioService
                if (svc != null && uiState.value.isConnected) {
                    svc.sendPositionBeacon()
                    ToneHelper.playPttUp(vol)
                    android.widget.Toast.makeText(
                        this, "WAYPOINT · posición enviada al grupo", android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        this, "Sin radio · waypoint no transmitido", android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun toggleListenOnly() {
        listenOnly = !listenOnly
        _uiState.update { it.copy(isListenOnly = listenOnly) }
        ToneHelper.playPttUp(alertVolume / 100f)
        // Aviso del estado al que se pasa, para que el cambio sea inequívoco
        android.widget.Toast.makeText(
            this,
            if (listenOnly) "MODO SOLO ESCUCHA · TX deshabilitada" else "MODO NORMAL · TX habilitada",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        executor.execute {
            RadioServiceAccessor.getAppDb(viewModel)
                .saveAppSetting(AppSetting.SETTING_LISTEN_ONLY, listenOnly.toString())
        }
    }

    /**
     * Cartel cuando el usuario intenta una función de TX estando en modo escucha.
     * Evita que parezca que el botón (o la app) no responde.
     */
    private fun notifyListenOnlyBlocked() {
        android.widget.Toast.makeText(
            this,
            "MODO SOLO ESCUCHA · soltá el modo escucha para transmitir",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // DEMO: simula 3 integrantes del grupo alrededor de Villa Martelli
    private fun seedDemoGroupMembers() {
        val now = System.currentTimeMillis()
        val demo = listOf(
            GroupMember("LUCAS",  -34.5455, -58.5310, 420,  45f, now),
            GroupMember("ANA",    -34.5490, -58.5265, 780,  120f, now - 90_000L),
            GroupMember("DIEGO",  -34.5430, -58.5295, 1250, 300f, now - 4 * 60_000L)
        )
        _groupMembers.value = demo
    }

    // ── Chat VHF ──────────────────────────────────────────────────────
    private fun addChatMessage(fromAlias: String, text: String, outgoing: Boolean) {
        val msg = ChatMessage(
            id          = chatMessageIdCounter++,
            fromAlias   = fromAlias,
            text        = text,
            timestampMs = System.currentTimeMillis(),
            isOutgoing  = outgoing
        )
        _chatMessages.update { it + msg }
    }

    private fun addAlertToChat(
        type: AlertHelper.AlertType,
        fromAlias: String,
        outgoing: Boolean,
        lat: Double?,
        lon: Double?
    ) {
        val kind = when (type) {
            AlertHelper.AlertType.EMERGENCY -> ChatMessage.AlertKind.EMERGENCY
            AlertHelper.AlertType.STOP      -> ChatMessage.AlertKind.STOP
            AlertHelper.AlertType.REGROUP   -> ChatMessage.AlertKind.REGROUP
        }
        val body = when (type) {
            AlertHelper.AlertType.EMERGENCY -> "SOLICITO ASISTENCIA INMEDIATA"
            AlertHelper.AlertType.STOP      -> "DETENIDO - INCONVENIENTE EN RUTA"
            AlertHelper.AlertType.REGROUP   -> "REAGRUPAR - ESPERAR EN POSICIÓN"
        }
        val msg = ChatMessage(
            id          = chatMessageIdCounter++,
            fromAlias   = fromAlias,
            text        = body,
            timestampMs = System.currentTimeMillis(),
            isOutgoing  = outgoing,
            alertType   = kind,
            lat         = lat,
            lon         = lon
        )
        _chatMessages.update { it + msg }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): android.location.Location? {
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return null
        return try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }
    }

    private fun sendChatMessage(text: String) {
        // En modo escucha no se transmite texto
        if (listenOnly) { ToneHelper.playAlertBeep(alertVolume / 100f); notifyListenOnlyBlocked(); return }
        addChatMessage(userAlias, text, outgoing = true)
        val svc = radioService
        if (svc != null && uiState.value.isConnected) {
            svc.sendChatMessage("CQ", text)
        }
        // Sin radio: el mensaje queda local (modo simulación)
    }

    private fun tuneToChannel(freq: String) {
        val mem = viewModel.channelMemories.value?.firstOrNull { it.frequency == freq }
        if (mem != null) {
            radioService?.tuneToMemory(mem)
        } else {
            radioService?.tuneToFreq(freq)
        }
        activeFrequencyStr = freq
        val name = mem?.name ?: "SIMPLEX"
        _uiState.update { it.copy(activeFrequency = freq, activeChannelName = name) }
    }

    // ── Alert flow ────────────────────────────────────────────────────
    private fun showAlertDialog(type: AlertHelper.AlertType) {
        pendingAlertType = type
        requestLocationAndTransmit(type)
    }

    private fun requestLocationAndTransmit(type: AlertHelper.AlertType) {
        pendingAlertType = type
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            // Ya hay permiso: dispara directo
            transmitGroupAlert(type)
            pendingAlertType = null
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun transmitGroupAlert(type: AlertHelper.AlertType) {
        // 1. Posición actual (puede ser null si aún no hay fix GPS)
        val loc = getLastKnownLocation()
        val lat = loc?.latitude
        val lon = loc?.longitude

        // 2. Siempre registrar la alerta en el chat (con o sin radio)
        addAlertToChat(type, userAlias, outgoing = true, lat = lat, lon = lon)
        ToneHelper.playEmergencyBeep(alertVolume / 100f)

        // 3. Transmitir por radio si está conectada
        val service = radioService
        if (service != null && uiState.value.isConnected) {
            val homeFreq   = activeFrequencyStr  // canal de grupo donde estaba
            val targetFreq = AlertHelper.getTargetFrequency(type, activeFrequencyStr)

            // Solo EMERGENCY cambia de frecuencia (a 140.970). STOP/REGROUP
            // se transmiten en el canal de grupo/alternativo actual.
            val needsFreqChange = targetFreq != homeFreq
            if (needsFreqChange) service.tuneToFreq(targetFreq)

            service.sendPositionBeacon()  // baliza en la frecuencia destino
            Handler(Looper.getMainLooper()).postDelayed({
                val alertText = AlertHelper.buildMessage(type, callsign)
                service.sendChatMessage("CQ", alertText)

                // Tras emitir la emergencia en 140.970, volver al canal de grupo
                // para seguir escuchando al resto del grupo.
                if (needsFreqChange) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        service.tuneToFreq(homeFreq)
                        activeFrequencyStr = homeFreq
                        _uiState.update { it.copy(activeFrequency = homeFreq) }
                    }, 1500)
                }
            }, 2000)
        }
    }
}
