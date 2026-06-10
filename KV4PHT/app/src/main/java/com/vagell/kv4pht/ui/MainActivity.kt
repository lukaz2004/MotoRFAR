package com.vagell.kv4pht.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vagell.kv4pht.ui.compose.MainScreen
import com.vagell.kv4pht.ui.compose.state.MainUiState
import com.vagell.kv4pht.ui.compose.theme.AppTheme
import com.vagell.kv4pht.ui.compose.theme.MotoRFARTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Fase A: estado hardcodeado. Fase B conecta al RadioAudioService.
            MotoRFARTheme(AppTheme.GREEN) {
                MainScreen(
                    state    = MainUiState.preview(),
                    onAction = { /* Fase B */ }
                )
            }
        }
    }
}
