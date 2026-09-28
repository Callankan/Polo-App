package com.callankan.poloapp

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Abre la app en un dispositivo real y comprueba que la interfaz arranca sin cerrarse. */
@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {
    @Test
    fun mainScreenStaysOpen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(6_000)
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
