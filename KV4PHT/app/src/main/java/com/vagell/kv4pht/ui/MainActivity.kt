package com.vagell.kv4pht.ui

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
import com.vagell.kv4pht.data.AppSetting
import com.vagell.kv4pht.data.ChannelMemory
import com.vagell.kv4pht.radio.RadioAudioService
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vagell.kv4pht.R
import com.vagell.kv4pht.ui.compose.AliasSettingScreen
import com.vagell.kv4pht.ui.compose.MainScreen
import com.vagell.kv4pht.ui.compose.MapScreen
import com.vagell.kv4pht.ui.compose.components.EmergencyConfirmDialog
import com.vagell.kv4pht.ui.compose.state.GroupMember
import com.vagell.kv4pht.ui.compose.state.MainUiAction
import com.vagell.kv4pht.ui.compose.state.MainUiState
import com.vagell.kv4pht.ui.compose.theme.AppTheme
import com.vagell.kv4pht.ui.compose.theme.EmergencyText
import com.vagell.kv4pht.ui.compose.theme.LocalMotoRFARColors
import com.vagell.kv4pht.ui.compose.theme.MotoRFARTheme
import com.vagell.kv4pht.ui.compose.GpsBeaconManager
import com.vagell.kv4pht.aprs.parser.APRSPacket
import com.vagell.kv4pht.aprs.parser.APRSTypes
import com.vagell.kv4pht.aprs.parser.MessagePacket
import com.vagell.kv4pht.aprs.parser.PositionField
import com.vagell.kv4pht.ui.compose.state.ReceivedAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())

    private var showEmergencyDialog by mutableStateOf(false)
    private var pendingAlertType: AlertHelper.AlertType? = null

    private val _beaconIntervalFlow = MutableStateFlow(60_000L)

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val beaconManager by lazy {
        GpsBeaconManager(
            onSendBeacon = { radioService?.sendPositionBeacon() },
            intervalFlow = _beaconIntervalFlow
        )
    }

    private val executor = Executors.newSingleThreadExecutor()

    // ── Permissions ──────────────────────────────────────────────────
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingAlertType?.let { transmitGroupAlert(it) }
        pendingAlertType = null
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
                ToneHelper.playStaticBurst(alertVolume / 100f)
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
                    _uiState.update { it.copy(activeAlert = alert) }
                    ToneHelper.playAlertBeep(alertVolume / 100f)
                    Handler(Looper.getMainLooper()).postDelayed({
                        _uiState.update { if (it.activeAlert?.fromAlias == alias) it.copy(activeAlert = null) else it }
                    }, 30_000L)
                }
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.loadDataAsync { loadSettings() }

        observeChannelMemories()

        setContent {
            val state by uiState.collectAsState()
            val groupMembers by _groupMembers.collectAsState()
            MotoRFARTheme(AppTheme.GREEN) {
                val colors = LocalMotoRFARColors.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "main"

                Scaffold(
                    containerColor = colors.background,
                    bottomBar = {
                        NavigationBar(containerColor = colors.surface) {
                            NavigationBarItem(
                                selected = currentRoute == "main",
                                onClick  = { navController.navigate("main") { launchSingleTop = true } },
                                icon     = { Icon(painterResource(R.drawable.ic_radio), contentDescription = "PTT") },
                                label    = { androidx.compose.material3.Text("PTT", color = colors.textSecondary, fontFamily = com.vagell.kv4pht.ui.compose.theme.ShareTechMono) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "map",
                                onClick  = { navController.navigate("map") { launchSingleTop = true } },
                                icon     = { Icon(painterResource(R.drawable.ic_pin), contentDescription = "MAPA") },
                                label    = { androidx.compose.material3.Text("MAPA", color = colors.textSecondary, fontFamily = com.vagell.kv4pht.ui.compose.theme.ShareTechMono) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "settings",
                                onClick  = { navController.navigate("settings") { launchSingleTop = true } },
                                icon     = { Icon(painterResource(R.drawable.ic_settings), contentDescription = "CONFIG") },
                                label    = { androidx.compose.material3.Text("CONFIG", color = colors.textSecondary, fontFamily = com.vagell.kv4pht.ui.compose.theme.ShareTechMono) }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController  = navController,
                        startDestination = "main",
                        modifier       = Modifier.padding(innerPadding)
                    ) {
                        composable("main") {
                            MainScreen(
                                state          = state,
                                onAction       = ::handleAction,
                                onDismissAlert = { _uiState.update { it.copy(activeAlert = null) } }
                            )
                        }
                        composable("map") {
                            MapScreen(groupMembers = groupMembers)
                        }
                        composable("settings") {
                            AliasSettingScreen(
                                currentAlias             = userAlias,
                                currentBeaconIntervalSec = beaconIntervalSec,
                                currentVolume            = alertVolume,
                                onSave                   = ::saveAliasSettings
                            )
                        }
                    }
                }
                if (showEmergencyDialog) {
                    EmergencyConfirmDialog(
                        onConfirm = {
                            showEmergencyDialog = false
                            requestLocationAndTransmit(AlertHelper.AlertType.EMERGENCY)
                        },
                        onDismiss = { showEmergencyDialog = false }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startAndBindService()
        beaconManager.start(serviceScope)
    }

    override fun onStop() {
        super.onStop()
        beaconManager.stop()
        if (radioServiceBound) {
            unbindService(serviceConnection)
            radioServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        executor.shutdownNow()
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
        _beaconIntervalFlow.value = beaconIntervalSec * 1000L
        runOnUiThread {
            _uiState.update { it.copy(activeFrequency = activeFrequencyStr) }
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
            MainUiAction.PttPressed    -> { ToneHelper.playPttDown(vol); radioService?.startPtt() }
            MainUiAction.PttReleased   -> { ToneHelper.playPttUp(vol);   radioService?.endPtt() }
            is MainUiAction.ChannelSelected -> tuneToChannel(action.freq)
            MainUiAction.EmergencyAlert -> { ToneHelper.playEmergencyBeep(vol); showEmergencyDialog = true }
            MainUiAction.StopAlert     -> { ToneHelper.playAlertBeep(vol); showAlertDialog(AlertHelper.AlertType.STOP) }
            MainUiAction.RegroupAlert  -> { ToneHelper.playAlertBeep(vol); showAlertDialog(AlertHelper.AlertType.REGROUP) }
        }
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
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private fun transmitGroupAlert(type: AlertHelper.AlertType) {
        val service = radioService ?: return
        val targetFreq = AlertHelper.getTargetFrequency(type, activeFrequencyStr)
        service.tuneToFreq(targetFreq)
        service.sendPositionBeacon()
        Handler(Looper.getMainLooper()).postDelayed({
            val alertText = AlertHelper.buildMessage(type, callsign)
            service.sendChatMessage("CQ", alertText)
        }, 2000)
    }
}
