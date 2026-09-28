package com.callankan.poloapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callankan.poloapp.data.settings.ThemeMode
import com.callankan.poloapp.feature.finance.SessionLock
import com.callankan.poloapp.navigation.PoloNavHost
import com.callankan.poloapp.notifications.ReminderScheduler
import com.callankan.poloapp.ui.theme.PoloTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var sessionLock: SessionLock

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.state.value is MainState.Loading }
        scheduler.schedule()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val ready = state as? MainState.Ready ?: return@setContent
            val settings = ready.settings
            val dark = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            if (ready.hasVehicle) NotificationPermissionOnce()
            PoloTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    key(ready.hasVehicle) { PoloNavHost(hasVehicle = ready.hasVehicle) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Revisa avisos y refresca el widget cada vez que se abre la app.
        scheduler.runNow()
    }

    override fun onStop() {
        super.onStop()
        sessionLock.lock()
        scheduler.runNow()
    }

    @androidx.compose.runtime.Composable
    private fun NotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted && !prefs.getBoolean("asked_notifications", false)) {
                prefs.edit().putBoolean("asked_notifications", true).apply()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
