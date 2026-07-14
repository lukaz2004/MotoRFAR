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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Observer
import ar.motorfar.app.data.AppSetting
import ar.motorfar.app.data.ChannelMemory
import ar.motorfar.app.radio.RadioAudioService
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import ar.motorfar.app.ui.compose.state.isEmergencyActive
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import ar.motorfar.app.ui.OnboardingHelper
import ar.motorfar.app.ui.onboarding.OnboardingActivity
import android.view.WindowManager
import java.util.concurrent.Executors

// 2026-07-09: LocationManager.getLastKnownLocation() puede devolver un fix
// cacheado en (0.0, 0.0) en vez de null cuando el GPS todavía no tuvo una
// lectura real -- eso pasaba el chequeo "!= null" y aparecía como
// "POS: 0.00000, 0.00000" en el chat de alertas. (0,0) es un punto real
// (Golfo de Guinea) pero implausible para este uso, así que se descarta.
fun isUsableLocation(lat: Double, lon: Double): Boolean = !(lat == 0.0 && lon == 0.0)

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
    private var manDownEnabled: Boolean = false

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
                this, getString(R.string.route_mode_off_auto), android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    // POIs propios compartidos al grupo -- reusa el chat VHF existente (sendChatMessage
    // a "CQ") en vez de un tipo de paquete APRS nuevo: la librería APRS vendorizada trae
    // un ObjectField incompleto (no serializa posición/timestamp reales), arreglarlo bien
    // es trabajo de protocolo aparte. Este formato de texto es propio de esta app, no
    // necesita interoperar con software APRS genérico.
    private val _pois = MutableStateFlow<List<ar.motorfar.app.ui.compose.state.PoiMarker>>(emptyList())
    private val poiMessageRegex = Regex("^📍 POI: (.+) · (-?\\d+\\.\\d+),(-?\\d+\\.\\d+)$")

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var chatMessageIdCounter = 0L

    // Punto al que centrar el mapa al tocar "ir a ubicación" en una alerta del chat
    private val _mapFocus = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _routePoints = MutableStateFlow<List<ar.motorfar.app.data.RoutePoint>>(emptyList())
    private var currentRouteSessionId: Long = 0L

    // 2026-07-06: Man-Down (acelerometro, cuenta regresiva, disparo de la alerta)
    // se movio a RadioAudioService -- vivia atado al ciclo de vida de esta
    // Activity y se apagaba justo al salir a segundo plano/pantalla apagada,
    // el escenario real para el que existe. _countdownValue se mantiene solo
    // como espejo de UI, alimentado por el callback manDownCountdownTick().
    private val _countdownValue = MutableStateFlow<Int?>(null)

    // 2026-07-06: si nos abrió el full-screen intent de Man-Down, el cartel de
    // recordatorio del canal de emergencia no debe taparle la pantalla a la
    // cuenta regresiva. Va en un StateFlow (no un `remember` local) porque si
    // la Activity ya estaba viva, Android reentrega el intent por
    // onNewIntent() en vez de recrear la Composition — un `remember` no se
    // enteraría de ese segundo aviso.
    private val _showEmergencyReminder = MutableStateFlow(true)

    private var pendingAlertType: AlertHelper.AlertType? = null

    // 2026-07-06: confirmacion real para STOP/REGROUP (un tap simple, sin hold,
    // podia mandar una falsa alarma al grupo por error). EMERGENCY no pasa por
    // aca — ya tiene su propia confirmacion deliberada (hold de 2s en
    // EmergencyConfirmButton), agregar un dialogo ahi seria friccion redundante.
    private val _pendingConfirmAlert = MutableStateFlow<AlertHelper.AlertType?>(null)

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
    // 2026-07-07: LocationListener por SAM/lambda depende de que onProviderEnabled/
    // onProviderDisabled/onStatusChanged sean "default" en la interfaz -- eso recien
    // existe desde API 30. En Android 7 (minSdk 24) esos metodos son abstractos de
    // verdad y el framework SI los llama, así que la clase generada por el lambda
    // (que solo implementa onLocationChanged) explota con AbstractMethodError apenas
    // el sistema dispara onProviderDisabled. object explícito con los 4 métodos
    // implementados anda en cualquier API.
    private val movementListener = object : android.location.LocationListener {
        override fun onLocationChanged(loc: android.location.Location) {
            // speed viene en m/s → km/h
            currentSpeedKmh = (loc.speed * 3.6).toDouble()
            if (loc.hasBearing() && loc.speed > 0.5f) {
                currentHeadingDeg = loc.bearing.toDouble()
                _uiState.update { it.copy(headingDeg = loc.bearing) }
            }

            // Registro de ruta: guarda punto si te moviste > 20m del último
            if (uiState.value.isRouteActive) {
                saveRoutePoint(loc.latitude, loc.longitude)
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

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in API 29+, pero sigue siendo abstracto en API < 30")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
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

    // 2026-07-10: POST_NOTIFICATIONS ya estaba declarado en el manifest, pero
    // eso no alcanza en Android 13+ (API 33) -- sin pedirlo en runtime, la
    // cuenta regresiva de Man-Down, las alertas del grupo, y la confirmación
    // de envío pueden simplemente no aparecer nunca, sin ningún indicio.
    // Pedido contextual (recién al activar Man-Down), mismo criterio que el
    // resto de permisos en esta app.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* el resultado no cambia el flujo -- si lo niega, Man-Down igual
           queda activado (sigue transmitiendo), solo no va a poder avisar
           con notificaciones. Nada más que hacer acá. */ }

    // ── ServiceConnection ─────────────────────────────────────────────
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as RadioAudioService.RadioBinder).service
            radioService = service
            radioServiceBound = true
            RadioServiceAccessor.setCallbacks(service, serviceCallbacks)
            service.start() // CRITICAL: Start the service connection controller
            // 2026-07-07: NO seteamos isConnected=true acá -- esto solo confirma que
            // el Service de Android está bindeado, no que el radio terminó el
            // handshake (HELLO, validación de firmware, mode->RX). isConnected
            // gatea si se llama a svc.startPtt() en el handler de PTT; seteado acá
            // dejaba la UI mostrando "conectado" mientras el radio todavía no
            // podía transmitir, y cualquier press en esa ventana se rechazaba en
            // silencio (o con el toast RADIO OCUPADA). radioConnected() ya lo marca
            // true en el momento correcto, cuando el handshake realmente terminó.
            // 2026-07-06: Man-Down vive en el Service (sigue en segundo plano) --
            // se activa acá según el setting guardado, no atado a onStart().
            serviceScope.launch(Dispatchers.IO) {
                val manDownEnabled = RadioServiceAccessor.getAppDb(viewModel).appSettingDao()
                    .getByName(AppSetting.SETTING_MAN_DOWN)
                    ?.value?.toBoolean() ?: false
                withContext(Dispatchers.Main) {
                    service.setManDownEnabled(manDownEnabled)
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            radioService = null
            radioServiceBound = false
            _uiState.update { it.copy(isConnected = false) }
        }
    }

    private val serviceShutdownReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (RadioAudioService.ACTION_SERVICE_STOPPING == intent?.action) {
                finish()
            }
        }
    }

    // ── RadioAudioService Callbacks ───────────────────────────────────
    private val serviceCallbacks = object : RadioAudioService.RadioAudioServiceCallbacks {
        // 2026-07-06: espejo de UI del countdown de Man-Down, que ahora vive y
        // corre en el Service (sigue funcionando aunque esto no esté bindeado).
        override fun manDownCountdownTick(secondsRemaining: Int?) {
            _countdownValue.value = secondsRemaining
        }
        override fun radioConnected() {
            _uiState.update { it.copy(isConnected = true) }
        }
        override fun radioMissing() {
            _uiState.update { it.copy(isConnected = false, sMeterLevel = 0) }
        }
        override fun wifiLinkChanged(up: Boolean) {
            _uiState.update { it.copy(isWifiLinkUp = up) }
        }
        // 2026-07-10: "CONECTADO" en pantalla solo confirma que el socket
        // existe, no que el equipo sigue respondiendo -- este aviso es lo
        // unico que hoy puede detectar un enlace colgado. No lo marcamos
        // isConnected=false (no calibrado con hardware real, ver comentario
        // en RadioAudioService) -- solo un aviso para que el usuario sepa
        // desconfiar y probar reconectar a mano.
        override fun connectionMaybeStale() {
            android.widget.Toast.makeText(
                this@MainActivity,
                "El equipo no responde hace un rato -- puede estar colgado. Probá reconectar.",
                android.widget.Toast.LENGTH_LONG
            ).show()
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
            // 2026-07-13: RadioAudioService llama a este callback en CADA
            // DeviceState que llega (sin gate de mode/hello) -- sin radio real
            // conectado, el equipo siempre reporta txActive=false, así que
            // "!txActive" solo (sin mirar el estado previo) tocaba el roger
            // beep en cada paquete, en loop infinito (encontrado en vivo: bips
            // sin parar en cualquier pantalla, con mode ya en BAD_FIRMWARE).
            // Ahora solo suena en la transición real true→false.
            if (!txActive && uiState.value.isTxActive) {
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
                    val poiMatch = poiMessageRegex.matchEntire(body)
                    if (poiMatch != null) {
                        val (label, latStr, lonStr) = poiMatch.destructured
                        val poi = ar.motorfar.app.ui.compose.state.PoiMarker(
                            label        = label,
                            lat          = latStr.toDouble(),
                            lon          = lonStr.toDouble(),
                            fromAlias    = alias,
                            receivedAtMs = System.currentTimeMillis()
                        )
                        _pois.update { it + poi }
                    }
                    // Mensaje de chat normal (o POI, que también se lee como texto
                    // si algo falla en el parseo de arriba) → al chat VHF
                    addChatMessage(alias, body, outgoing = false)
                    ToneHelper.playAlertBeep(alertVolume / 100f)
                }
            }
        }
    }

    // 2026-07-06: si nos abrió la notificación de Man-Down (full-screen intent),
    // mostrarnos encima de la pantalla bloqueada (mismo mecanismo que apps de
    // llamadas/alarmas) y no dejar que el cartel del canal de emergencia tape
    // la cuenta regresiva. Se llama desde onCreate() Y onNewIntent() — si la
    // Activity ya estaba viva, Android reentrega el intent por onNewIntent()
    // en vez de recrear la Activity.
    private fun handleManDownFullscreenIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(RadioAudioService.EXTRA_MANDOWN_FULLSCREEN, false) == true) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            _showEmergencyReminder.value = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleManDownFullscreenIntent(intent)
    }

    // 2026-07-06: cancelar Man-Down apretando VOLUMEN ABAJO 3 veces seguidas —
    // pensado para el caso real de una caída donde la pantalla se rompió o no
    // responde al tacto (guantes, agua, vidrio roto), pero las teclas físicas
    // siguen andando. Solo actúa si hay una cuenta regresiva activa; el resto
    // del tiempo el volumen se comporta normal.
    private val volumeDownPressTimestamps = mutableListOf<Long>()
    private val VOLUME_CANCEL_PRESSES = 3
    private val VOLUME_CANCEL_WINDOW_MS = 2500L

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN && _countdownValue.value != null) {
            val now = System.currentTimeMillis()
            volumeDownPressTimestamps.add(now)
            volumeDownPressTimestamps.removeAll { now - it > VOLUME_CANCEL_WINDOW_MS }
            if (volumeDownPressTimestamps.size >= VOLUME_CANCEL_PRESSES) {
                volumeDownPressTimestamps.clear()
                cancelFallCountdown()
            }
            return true // no toques el volumen real mientras hay cuenta regresiva
        }
        return super.onKeyDown(keyCode, event)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ToneHelper.playAppStartupJingle(alertVolume / 100f)

        // 2026-07-06: si nos abrió la notificación de Man-Down (full-screen
        // intent), mostrarnos encima de la pantalla bloqueada — mismo
        // mecanismo que usan apps de llamadas/alarmas. Gateado por el extra
        // para no hacerlo en un arranque normal de la app.
        handleManDownFullscreenIntent(intent)

        // Registra el receiver para cerrar la app si el servicio se detiene
        val filter = android.content.IntentFilter(RadioAudioService.ACTION_SERVICE_STOPPING)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceShutdownReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceShutdownReceiver, filter)
        }

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

        // Sync local countdown state with UI state
        serviceScope.launch {
            _countdownValue.collect { value ->
                _uiState.update { it.copy(fallCountdown = value) }
            }
        }

        // DEMO: miembros de grupo simulados para visualizar el mapa sin hardware.
        // (solo en debug — quitar/condicionar para producción)
        if (ar.motorfar.app.BuildConfig.DEBUG) {
            seedDemoGroupMembers()
        }

        setContent {
            val state by uiState.collectAsState()
            val groupMembers by _groupMembers.collectAsState()
            val pois by _pois.collectAsState()
            val chatMessages by _chatMessages.collectAsState()
            val mapFocus by _mapFocus.collectAsState()
            val routePoints by _routePoints.collectAsState()

            MotoRFARTheme(state.theme) {
                val colors = LocalMotoRFARColors.current

                // 2026-07-06: recordatorio del uso exclusivo del canal de emergencia.
                // Vive en _showEmergencyReminder (StateFlow de la Activity, no un
                // `remember` local) para que onNewIntent() lo pueda apagar también
                // si Man-Down nos reabre con la Activity ya viva.
                val showEmergencyReminder by _showEmergencyReminder.collectAsState()
                if (showEmergencyReminder) {
                    AlertDialog(
                        onDismissRequest = { _showEmergencyReminder.value = false },
                        title = { Text("⚠ Canal de Emergencia (140.970 MHz)") },
                        text = {
                            Text(
                                "Es de uso EXCLUSIVO para emergencias reales, regido por " +
                                "la Res. 5/2015 (ENACOM / Secretaría de Comunicaciones). " +
                                "No lo dejes sintonizado para chatear — la app ya bloquea " +
                                "el chat de texto y los avisos STOP/Reagrupamiento ahí; la " +
                                "voz por PTT queda a tu criterio y responsabilidad."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { _showEmergencyReminder.value = false }) {
                                Text("Entendido")
                            }
                        }
                    )
                }

                // 2026-07-06: confirmacion real antes de mandar STOP/REGROUP (texto ya
                // existia en AlertHelper pero nunca se conectaba a ningun dialogo).
                val pendingConfirmAlert by _pendingConfirmAlert.collectAsState()
                pendingConfirmAlert?.let { type ->
                    AlertDialog(
                        onDismissRequest = { _pendingConfirmAlert.value = null },
                        title = { Text(AlertHelper.getConfirmationTitle(type)) },
                        text = { Text(AlertHelper.getConfirmationText(type)) },
                        confirmButton = {
                            TextButton(onClick = {
                                _pendingConfirmAlert.value = null
                                requestLocationAndTransmit(type)
                            }) { Text("Confirmar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { _pendingConfirmAlert.value = null }) { Text("Cancelar") }
                        }
                    )
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "main"
                val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                // 2026-07-07: el overlay de Man-Down vivía adentro de MainScreen
                // (solo la ruta "main") -- en Mapa/Chat/Ajustes solo sonaba el beep
                // (que corre en el Service, independiente de la UI) pero no se veía
                // ningún cartel. Envuelto en un Box afuera del Scaffold para que se
                // vea sin importar en qué pantalla estés.
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = colors.background,
                    bottomBar = {
                        // En horizontal la navegación va en un NavigationRail lateral (libera alto)
                        // ponytail: NavigationBar de Material3 fuerza 80dp de alto fijo pase lo
                        // que pase adentro -- eso era el espacio vacío debajo de MAPA/CHAT. Row
                        // simple sin ese piso, altura real de contenido (52dp).
                        if (!isLandscape) {
                        // ponytail: el verde tiene que pintar también debajo, atrás de la
                        // barra gestual -- background()+navigationBarsPadding() en el mismo
                        // Row no alcanzaba (el Scaffold no le da crédito del inset). Column
                        // con el fondo afuera + un Spacer del alto exacto del inset resuelve
                        // sin ambigüedad: el verde cubre Row + inset, sin cortarse.
                        Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    Triple("main", R.drawable.ic_mic, "PTT"),
                                    Triple("map", R.drawable.ic_pin, "MAPA"),
                                    Triple("chat", R.drawable.ic_text_chat_mode, "CHAT")
                                ).forEach { (route, iconRes, label) ->
                                    if (currentRoute != route) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement  = Arrangement.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable { navController.navigate(route) { launchSingleTop = true } }
                                        ) {
                                            Icon(painterResource(iconRes), contentDescription = label, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                                            androidx.compose.material3.Text(label, color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            )
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
                                    icon     = { Icon(painterResource(R.drawable.ic_mic), contentDescription = "PTT", modifier = Modifier.size(30.dp)) },
                                    label    = { androidx.compose.material3.Text("PTT", color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono, fontSize = 20.sp) }
                                )
                            }
                            if (currentRoute != "map") {
                                NavigationRailItem(
                                    selected = false,
                                    onClick  = { navController.navigate("map") { launchSingleTop = true } },
                                    icon     = { Icon(painterResource(R.drawable.ic_pin), contentDescription = "MAPA", modifier = Modifier.size(30.dp)) },
                                    label    = { androidx.compose.material3.Text("MAPA", color = colors.textSecondary, fontFamily = ar.motorfar.app.ui.compose.theme.ShareTechMono, fontSize = 20.sp) }
                                )
                            }
                            if (currentRoute != "chat") {
                                NavigationRailItem(
                                    selected = false,
                                    onClick  = { navController.navigate("chat") { launchSingleTop = true } },
                                    icon     = { Icon(painterResource(R.drawable.ic_text_chat_mode), contentDescription = "CHAT", modifier = Modifier.size(30.dp)) },
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
                                state              = state,
                                onAction           = ::handleAction,
                                onDismissAlert     = { _uiState.update { it.copy(activeAlert = null) } },
                                onOpenSettings     = { navController.navigate("settings") { launchSingleTop = true } },
                                onOpenWifiSettings = { navController.navigate("wifi") { launchSingleTop = true } }
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
                                pois            = pois,
                                routePoints     = routePoints,
                                locationGranted = state.locationGranted,
                                headingDeg      = state.headingDeg,
                                focusTarget     = mapFocus,
                                onFocusConsumed = { _mapFocus.value = null },
                                isTransmitting  = state.isTxActive,
                                listenOnly      = state.isListenOnly,
                                isEmergency     = state.isEmergencyActive,
                                onPttDown       = { handleAction(MainUiAction.PttPressed) },
                                onPttUp         = { handleAction(MainUiAction.PttReleased) },
                                onSendWaypoint  = { handleAction(MainUiAction.SendWaypoint) },
                                onSendPoi       = { label, lat, lon -> performSendPoi(label, lat, lon) }
                            )
                        }
                        composable("settings") {
                            AliasSettingScreen(
                                currentAlias             = userAlias,
                                currentBeaconIntervalSec = beaconIntervalSec,
                                currentVolume            = alertVolume,
                                currentSmartBeacon       = smartBeaconEnabled,
                                currentManDown           = manDownEnabled,
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
                                onToggleManDown          = { enabled ->
                                    manDownEnabled = enabled
                                    radioService?.setManDownEnabled(enabled)
                                    if (enabled &&
                                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    executor.execute {
                                        RadioServiceAccessor.getAppDb(viewModel)
                                            .saveAppSetting(AppSetting.SETTING_MAN_DOWN, enabled.toString())
                                    }
                                },
                                onConfigureTones         = {
                                    navController.navigate("tones") { launchSingleTop = true }
                                },
                                onConfigureWifi          = {
                                    navController.navigate("wifi") { launchSingleTop = true }
                                },
                                onConfigureOfflineMaps   = {
                                    navController.navigate("offline_maps") { launchSingleTop = true }
                                },
                                onOpenRouteHistory       = {
                                    navController.navigate("route_history") { launchSingleTop = true }
                                },
                                onClearRoute             = { clearRoute() },
                                onPrivacyPolicy          = {
                                    startActivity(android.content.Intent(this@MainActivity, PrivacyPolicyActivity::class.java))
                                },
                                onAbout                  = {
                                    startActivity(android.content.Intent(this@MainActivity, AboutActivity::class.java))
                                }
                            )
                        }
                        composable("tones") {
                            // 2026-07-06: no depende de state.channels (cadena Room ->
                            // LiveData -> StateFlow -> Compose, con timing variable en
                            // una instalacion nueva de verdad) -- lee la DB directo cada
                            // vez que se entra a esta pantalla, para no quedar en blanco.
                            var toneScreenChannels by remember { mutableStateOf<List<ar.motorfar.app.data.ChannelMemory>>(emptyList()) }
                            LaunchedEffect(Unit) {
                                toneScreenChannels = withContext(Dispatchers.IO) {
                                    RadioServiceAccessor.getAppDb(viewModel).channelMemoryDao().getAll()
                                }
                            }
                            ar.motorfar.app.ui.compose.TonesSettingScreen(
                                channels       = toneScreenChannels,
                                onToneSelected = { memoryId, tone ->
                                    serviceScope.launch(Dispatchers.IO) {
                                        val dao = RadioServiceAccessor.getAppDb(viewModel).channelMemoryDao()
                                        dao.getById(memoryId)?.let { memory ->
                                            memory.txTone = tone
                                            memory.rxTone = tone
                                            dao.update(memory)
                                        }
                                        val fresh = dao.getAll()
                                        withContext(Dispatchers.Main) { toneScreenChannels = fresh }
                                    }
                                },
                                onBack         = { navController.popBackStack() }
                            )
                        }
                        composable("wifi") {
                            ar.motorfar.app.ui.compose.WifiSettingScreen(
                                isConnected    = state.isWifiLinkUp,
                                onSavePassword = { pw -> radioService?.setWifiPassword(pw) ?: false },
                                onSaveSsid     = { ssid -> radioService?.setWifiSsid(ssid) ?: false },
                                onBack         = { navController.popBackStack() }
                            )
                        }
                        composable("offline_maps") {
                            ar.motorfar.app.ui.compose.OfflineVectorMapsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("route_history") {
                            ar.motorfar.app.ui.compose.RouteHistoryScreen(
                                alias = userAlias,
                                onBack = { navController.popBackStack() },
                                onViewSession = { sessionId, points ->
                                    currentRouteSessionId = sessionId
                                    _routePoints.value = points
                                    navController.navigate("map") { launchSingleTop = true }
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

                state.fallCountdown?.let { seconds ->
                    ar.motorfar.app.ui.compose.components.FallCountdownOverlay(
                        secondsLeft = seconds,
                        onCancel    = { cancelFallCountdown() }
                    )
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
        // Man-Down se activa en onServiceConnected() (vive en el Service, no acá).
    }

    override fun onStop() {
        super.onStop()
        beaconManager.stop()
        stopMovementUpdates()
        // Man-Down NO se detiene acá a propósito — debe seguir funcionando con
        // la app en segundo plano/pantalla apagada, que es el uso real.
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
        unregisterReceiver(serviceShutdownReceiver)
        serviceScope.cancel()
        executor.shutdownNow()
        routeAutoOffHandler.removeCallbacks(routeAutoOffRunnable)
    }

    private fun saveRoutePoint(lat: Double, lon: Double) {
        val lastPoint = _routePoints.value.lastOrNull()
        if (lastPoint != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(lastPoint.latitude, lastPoint.longitude, lat, lon, results)
            if (results[0] < 20f) return // Solo guardar si nos movimos > 20m
        }

        val point = ar.motorfar.app.data.RoutePoint(
            timestamp = System.currentTimeMillis(),
            latitude  = lat,
            longitude = lon,
            alias     = userAlias,
            sessionId = currentRouteSessionId
        )
        _routePoints.update { it + point }
        executor.execute {
            RadioServiceAccessor.getAppDb(viewModel).routePointDao().insert(point)
        }
    }

    private fun clearRoute() {
        _routePoints.value = emptyList()
        executor.execute {
            RadioServiceAccessor.getAppDb(viewModel).routePointDao().deleteForAlias(userAlias)
        }
    }

    // ── Fall Detection ───────────────────────────────────────────────
    // 2026-07-06: la cuenta regresiva, el disparo de la alerta y el foco de
    // audio se movieron a RadioAudioService (ver setManDownEnabled/
    // cancelManDownCountdown ahí) — acá solo queda la cancelación manual,
    // que delega al Service. _countdownValue se actualiza vía el callback
    // manDownCountdownTick() cuando la Activity está bindeada.
    private fun cancelFallCountdown() {
        radioService?.cancelManDownCountdown()
    }

    // ── Service binding ───────────────────────────────────────────────
    private fun startAndBindService() {
        val effectiveCallsign = userAlias.ifBlank { callsign }
        val intent = Intent(this, RadioAudioService::class.java)
            .putExtra(AppSetting.SETTING_CALLSIGN, effectiveCallsign)
            .putExtra("activeMemoryId", activeMemoryId)
            .putExtra("activeFrequencyStr", activeFrequencyStr)
        // 2026-07-07: Context.startForegroundService() es API 26+ -- no existe
        // como método en Android 7 (NoSuchMethodError, crasheaba la app al toque
        // en el Huawei P9). Pre-O, startService() ya alcanza: la promoción a
        // foreground la hace el propio Service llamando a startForeground()
        // (ese método sí existe desde mucho antes de API 26).
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun loadSettings() {
        val db = RadioServiceAccessor.getAppDb(viewModel)
        val settings = db.appSettingDao().getAll()
            .associateBy(AppSetting::name, AppSetting::value)
        
        // Carga los puntos de la última salida guardada (no todo el historial mezclado)
        executor.execute {
            val dao = db.routePointDao()
            val latestSession = dao.getLatestSessionId(userAlias)
            if (latestSession != null) {
                currentRouteSessionId = latestSession
                _routePoints.value = dao.getPointsForSession(userAlias, latestSession)
            }
        }

        callsign           = settings.getOrDefault(AppSetting.SETTING_CALLSIGN, "")
        activeFrequencyStr = settings.getOrDefault("activeFrequencyStr", "139.9700")
        userAlias          = settings.getOrDefault(AppSetting.SETTING_USER_ALIAS, "MOTO")
        beaconIntervalSec  = settings.getOrDefault(AppSetting.SETTING_BEACON_INTERVAL_SEC, "60").toIntOrNull() ?: 60
        alertVolume        = settings.getOrDefault(AppSetting.SETTING_ALERT_VOLUME, "70").toIntOrNull() ?: 70
        listenOnly         = settings.getOrDefault(AppSetting.SETTING_LISTEN_ONLY, "false").toBoolean()
        smartBeaconEnabled = settings.getOrDefault(AppSetting.SETTING_SMART_BEACON, "true").toBoolean()
        manDownEnabled     = settings.getOrDefault(AppSetting.SETTING_MAN_DOWN, "false").toBoolean()
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
                vibrate()
                // 2026-07-10: antes sonaba playPttDown() SIEMPRE, incluso si
                // startPtt() fallaba -- mismo tono para "transmitiendo" y para
                // "rechazado", igual que un HT real donde el tono de PTT
                // aceptado tiene que ser distinto del de canal ocupado/no
                // permitido. Ahora el tono de éxito solo suena si realmente
                // arrancó; si no, suena el mismo tono de denegado que usa el
                // resto de la app (modo escucha, canal Emergencia bloqueado).
                val svc = radioService
                if (svc != null && uiState.value.isConnected) {
                    if (svc.startPtt()) {
                        ToneHelper.playPttDown(vol)
                    } else {
                        ToneHelper.playAlertBeep(vol)
                        notifyPttNotAllowed()
                    }
                } else {
                    // Modo simulación: sin radio, solo feedback visual (anillos TX)
                    ToneHelper.playPttDown(vol)
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
            MainUiAction.EmergencyAlert -> { 
                vibrate()
                ToneHelper.playEmergencyBeep(vol)
                requestLocationAndTransmit(AlertHelper.AlertType.EMERGENCY) 
            }
            MainUiAction.StopAlert     -> {
                if (listenOnly) { ToneHelper.playAlertBeep(vol); notifyListenOnlyBlocked(); return }
                vibrate()
                ToneHelper.playAlertBeep(vol); showAlertDialog(AlertHelper.AlertType.STOP)
            }
            MainUiAction.RegroupAlert  -> {
                if (listenOnly) { ToneHelper.playAlertBeep(vol); notifyListenOnlyBlocked(); return }
                vibrate()
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
                    // Nueva salida: arranca una sesión propia, no sigue la línea de la anterior
                    currentRouteSessionId = System.currentTimeMillis()
                    _routePoints.value = emptyList()
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    stationaryStartMs = 0L
                    android.widget.Toast.makeText(
                        this, getString(R.string.route_mode_on), android.widget.Toast.LENGTH_SHORT
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
                
                val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasPerm) {
                    performSendWaypoint()
                } else {
                    // Reutilizamos el launcher de permisos para GPS
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            MainUiAction.CancelFallCountdown -> cancelFallCountdown()
            MainUiAction.ClearRoute -> clearRoute()
        }
    }

    private fun performSendWaypoint() {
        val vol = alertVolume / 100f
        val svc = radioService
        if (svc != null && uiState.value.isConnected) {
            svc.sendPositionBeacon()
            
            // También guardamos nuestro waypoint en la base de datos local
            getLastKnownLocation()?.let { saveRoutePoint(it.latitude, it.longitude) }

            ToneHelper.playPttUp(vol)
            android.widget.Toast.makeText(
                this, getString(R.string.waypoint_sent), android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            android.widget.Toast.makeText(
                this, getString(R.string.waypoint_no_radio), android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun performSendPoi(label: String, lat: Double, lon: Double) {
        val vol = alertVolume / 100f
        val svc = radioService
        if (svc != null && uiState.value.isConnected) {
            val text = "📍 POI: $label · $lat,$lon"
            svc.sendChatMessage(null, text)
            // Se agrega de una en el propio dispositivo -- no hay que esperar a
            // "recibir" el propio paquete para verlo en el mapa.
            _pois.update {
                it + ar.motorfar.app.ui.compose.state.PoiMarker(
                    label        = label,
                    lat          = lat,
                    lon          = lon,
                    fromAlias    = userAlias,
                    receivedAtMs = System.currentTimeMillis()
                )
            }
            ToneHelper.playPttUp(vol)
            android.widget.Toast.makeText(this, "POI enviado al grupo", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(
                this, getString(R.string.waypoint_no_radio), android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun toggleListenOnly() {
        listenOnly = !listenOnly
        _uiState.update { it.copy(isListenOnly = listenOnly) }
        ToneHelper.playPttUp(alertVolume / 100f)
        // Aviso del estado al que se pasa, para que el cambio sea inequívoco
        android.widget.Toast.makeText(
            this,
            if (listenOnly) getString(R.string.listen_only_on) else getString(R.string.listen_only_off),
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
            getString(R.string.listen_only_blocked),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 2026-07-07: startPtt() rechaza el press en silencio si el módulo todavía
     * no volvió a RX (timeout de runaway TX, etc.) -- antes no había ningún
     * aviso, se sentía como que el botón PTT a veces no respondía.
     */
    private fun notifyPttNotAllowed() {
        android.widget.Toast.makeText(
            this,
            getString(R.string.ptt_not_allowed),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // 2026-07-06: 140.970 es uso exclusivo de emergencias reales (Res. 5/2015) —
    // chat libre y avisos STOP/REAGRUPAMIENTO no deben salir por ese canal solo
    // porque el usuario lo dejó sintonizado ahí. La alerta real de EMERGENCY no
    // pasa por este aviso, sigue funcionando siempre.
    private fun notifyEmergencyChannelBlocked() {
        android.widget.Toast.makeText(
            this,
            getString(R.string.emergency_channel_blocked),
            android.widget.Toast.LENGTH_LONG
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
            val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            loc?.takeIf { isUsableLocation(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    private fun sendChatMessage(text: String) {
        // En modo escucha no se transmite texto
        if (listenOnly) { ToneHelper.playAlertBeep(alertVolume / 100f); notifyListenOnlyBlocked(); return }
        // Canal de emergencia: uso exclusivo M.T.T.T., no chat de rutina
        if (activeFrequencyStr == AlertHelper.EMERGENCY_FREQ) { ToneHelper.playAlertBeep(alertVolume / 100f); notifyEmergencyChannelBlocked(); return }
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
        _pendingConfirmAlert.value = type
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
        // 2026-07-06: STOP/REGROUP no son emergencias reales — no deben salir por
        // 140.970 solo porque el usuario dejó el radio sintonizado ahí. EMERGENCY
        // siempre pasa (fuerza su propia frecuencia en AlertHelper.getTargetFrequency).
        if (type != AlertHelper.AlertType.EMERGENCY && activeFrequencyStr == AlertHelper.EMERGENCY_FREQ) {
            ToneHelper.playAlertBeep(alertVolume / 100f)
            notifyEmergencyChannelBlocked()
            return
        }
        // 1. Posición actual (puede ser null si aún no hay fix GPS)
        val loc = getLastKnownLocation()
        val lat = loc?.latitude
        val lon = loc?.longitude

        // 2. Siempre registrar la alerta en el chat local (es tu propio registro
        // de que apretaste el botón), pero el aviso real de éxito/fracaso al
        // usuario depende de si había radio conectada -- antes esto no se
        // distinguía y quedaba la falsa impresión de que siempre se transmitió.
        addAlertToChat(type, userAlias, outgoing = true, lat = lat, lon = lon)
        ToneHelper.playEmergencyBeep(alertVolume / 100f)

        // 3. Transmitir por radio si está conectada
        val service = radioService
        if (service != null && uiState.value.isConnected) {
            android.widget.Toast.makeText(
                this, AlertHelper.getSentConfirmation(type), android.widget.Toast.LENGTH_SHORT
            ).show()
            val homeFreq        = activeFrequencyStr  // canal de grupo donde estaba
            val homeChannelName = uiState.value.activeChannelName
            val targetFreq       = AlertHelper.getTargetFrequency(type, activeFrequencyStr)

            // Solo EMERGENCY cambia de frecuencia (a 140.970). STOP/REGROUP
            // se transmiten en el canal de grupo/alternativo actual.
            val needsFreqChange = targetFreq != homeFreq
            if (needsFreqChange) {
                service.tuneToFreq(targetFreq)
                // 2026-07-06: antes solo se retunaba el hardware -- el estado
                // de la UI (frecuencia/nombre de canal) nunca se enteraba, así
                // que nada en pantalla (cartel rojo, botón PTT, ecualizador)
                // reflejaba que se estaba transmitiendo en Emergencia.
                activeFrequencyStr = targetFreq
                _uiState.update {
                    it.copy(activeFrequency = targetFreq, activeChannelName = "EMERGENCIA")
                }
            }

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
                        _uiState.update {
                            it.copy(activeFrequency = homeFreq, activeChannelName = homeChannelName)
                        }
                    }, 1500)
                }
            }, 2000)
        } else {
            android.widget.Toast.makeText(
                this, getString(R.string.alert_not_transmitted), android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}
