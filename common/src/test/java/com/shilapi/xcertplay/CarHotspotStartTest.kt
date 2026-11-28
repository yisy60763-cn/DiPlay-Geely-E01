package com.shilapi.xcertplay

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import com.shilapi.xcertplay.transport.WirelessHotspotMode

/**
 * Opening the E01 app never starts the retired wireless hotspot path, even if old preferences request it.
 *
 * The decision itself — that the switch is off by default and only means anything for the car's own
 * hotspot — is [CarHotspotAutoStartTest]. What this pins is the half a decision cannot show: that
 * the answer is acted on, off the main thread, and written where a report can read it.
 *
 * Only the switch-on direction is asserted. A negative here would be "nothing was logged in the time
 * we waited", which a slow machine turns into a passing test for the wrong reason; the switch-off
 * direction is the pure test's, where it is an equality rather than a race.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en")
class CarHotspotStartTest {
    @Test fun wiredOnlyStartupDoesNotAskForTheCarsHotspotEvenWhenLegacyPreferenceIsOn() {
        val app = RuntimeEnvironment.getApplication()
        // This is an integration test of the enabled path: do not rely on the default-off preference.
        AirPlayPersistence.saveWirelessEnabled(app, true)
        AirPlayPersistence.saveWirelessHotspotMode(app, WirelessHotspotMode.MANUAL)
        AirPlayPersistence.saveAutoCarHotspot(app, true)

        try {
            Robolectric.buildActivity(
                DiPlayActivity::class.java,
                Intent(app, DiPlayActivity::class.java),
            ).setup().get()

            assertFalse("wired-only startup must not attempt to start the car hotspot", hotspotAttemptWasLogged())
        } finally {
            AirPlayPersistence.saveAutoCarHotspot(app, false)
        }
    }

    /** The controller is polled off the main thread, so the line arrives after `setup` returns. */
    private fun hotspotAttemptWasLogged(): Boolean {
        val deadline = System.currentTimeMillis() + WAIT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val logged = ShadowLog.getLogsForTag("DiPlay-CarHotspot")
                .any { it.msg.startsWith("app-start hotspot") }
            if (logged) return true
            Thread.sleep(POLL_MILLIS)
        }
        return false
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
        const val POLL_MILLIS = 25L
    }
}
