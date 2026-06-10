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
import com.vagell.kv4pht.ui.compose.MainScreen
import com.vagell.kv4pht.ui.compose.state.MainUiAction
import com.vagell.kv4pht.ui.compose.state.MainUiState
import com.vagell.kv4pht.ui.compose.theme.AppTheme
import com.vagell.kv4pht.ui.compose.theme.EmergencyText
import com.vagell.kv4pht.ui.compose.theme.MotoRFARTheme
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

    private var showEmergencyDialog by mutableStateOf(false)
    private var pendingAlertType: AlertHelper.AlertType? = null

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
            _uiState.update { it.copy(isTxActive = txActive) }
        }
        override fun tunedToFreq(frequencyStr: String) {
            _uiState.update { it.copy(activeFrequency = frequencyStr) }
            activeFrequencyStr = frequencyStr
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
            MotoRFARTheme(AppTheme.GREEN) {
                MainScreen(state = state, onAction = ::handleAction)
                if (showEmergencyDialog) {
                    AlertDialog(
                        onDismissRequest = { showEmergencyDialog = false },
                        title   = { Text("⚠ EMERGENCIA", color = EmergencyText) },
                        text    = { Text(AlertHelper.getConfirmationText(AlertHelper.AlertType.EMERGENCY)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showEmergencyDialog = false
                                requestLocationAndTransmit(AlertHelper.AlertType.EMERGENCY)
                            }) { Text("TRANSMITIR", color = EmergencyText) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEmergencyDialog = false }) {
                                Text("CANCELAR")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startAndBindService()
    }

    override fun onStop() {
        super.onStop()
        if (radioServiceBound) {
            unbindService(serviceConnection)
            radioServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    // ── Service binding ───────────────────────────────────────────────
    private fun startAndBindService() {
        val intent = Intent(this, RadioAudioService::class.java)
            .putExtra(AppSetting.SETTING_CALLSIGN, callsign)
            .putExtra("activeMemoryId", activeMemoryId)
            .putExtra("activeFrequencyStr", activeFrequencyStr)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun loadSettings() {
        val settings = RadioServiceAccessor.getAppDb(viewModel).appSettingDao().getAll()
            .associateBy(AppSetting::name, AppSetting::value)
        callsign = settings.getOrDefault(AppSetting.SETTING_CALLSIGN, "")
        activeFrequencyStr = settings.getOrDefault("activeFrequencyStr", "139.9700")
        runOnUiThread {
            _uiState.update { it.copy(activeFrequency = activeFrequencyStr) }
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
        when (action) {
            MainUiAction.PttPressed    -> radioService?.startPtt()
            MainUiAction.PttReleased   -> radioService?.endPtt()
            is MainUiAction.ChannelSelected -> tuneToChannel(action.freq)
            MainUiAction.EmergencyAlert -> showEmergencyDialog = true
            MainUiAction.StopAlert     -> showAlertDialog(AlertHelper.AlertType.STOP)
            MainUiAction.RegroupAlert  -> showAlertDialog(AlertHelper.AlertType.REGROUP)
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
