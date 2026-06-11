package ar.motorfar.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ar.motorfar.app.data.AppDatabase
import ar.motorfar.app.data.AppSetting
import ar.motorfar.app.ui.MainActivity
import ar.motorfar.app.ui.TermsActivity
import ar.motorfar.app.ui.compose.theme.AppTheme
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme
import java.util.concurrent.Executors

class OnboardingActivity : ComponentActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotoRFARTheme(AppTheme.GREEN) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "terms") {
                    composable("terms") {
                        TermsScreen(
                            step     = 1,
                            onAccept = {
                                saveTermsAccepted()
                                navController.navigate("alias")
                            }
                        )
                    }
                    composable("alias") {
                        AliasSetupOnboarding(
                            step   = 2,
                            onSave = { alias ->
                                saveAlias(alias)
                                navController.navigate("channel")
                            }
                        )
                    }
                    composable("channel") {
                        ChannelSelectOnboarding(
                            step       = 3,
                            onComplete = { freq -> completeOnboarding(freq) }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    private fun saveTermsAccepted() {
        executor.execute {
            AppDatabase.getInstance(this).saveAppSetting(
                TermsActivity.TERMS_ACCEPTED_KEY,
                TermsActivity.TERMS_ACCEPTED_VALUE
            )
        }
    }

    private fun saveAlias(alias: String) {
        executor.execute {
            AppDatabase.getInstance(this)
                .saveAppSetting(AppSetting.SETTING_USER_ALIAS, alias)
        }
    }

    private fun completeOnboarding(freq: String) {
        executor.execute {
            val db = AppDatabase.getInstance(this)
            db.saveAppSetting("activeFrequencyStr", freq)
            db.saveAppSetting(AppSetting.SETTING_ONBOARDING_COMPLETE, "true")
            runOnUiThread {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
