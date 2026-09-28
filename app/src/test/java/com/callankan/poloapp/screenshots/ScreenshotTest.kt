package com.callankan.poloapp.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callankan.poloapp.data.settings.ThemeMode
import com.callankan.poloapp.feature.dashboard.DashboardContent
import com.callankan.poloapp.ui.theme.PoloTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de las pantallas con datos de ejemplo (Galaxy A55 ≈ 411 x 891 dp).
 * Generar con: ./gradlew recordRoborazziDebug
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String, theme: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
        compose.setContent {
            PoloTheme(themeMode = theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    /** Tema claro del dashboard con datos de ejemplo (la versión oscura sale del recorrido completo). */
    @Test
    fun dashboardLight() = capture("01b_dashboard_claro", ThemeMode.LIGHT) { DashboardContent(SampleData.overview(), {}, animate = false) }
}
