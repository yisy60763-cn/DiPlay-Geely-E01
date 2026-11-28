package com.shilapi.xcertplay

import android.content.Context
import android.content.Intent
import com.shilapi.xcertplay.carhop.CarBluetoothHop
import com.shilapi.xcertplay.carhop.CarBluetoothHops
import com.shilapi.xcertplay.carhop.CarBluetoothMaintenance
import com.shilapi.xcertplay.carhop.CarBluetoothPhone
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The E01 wired-only profile must not start the car Bluetooth hop at app launch.
 * USB CarPlay does not depend on the vehicle Bluetooth radio; waking it adds needless work
 * and can interfere with the vehicle's own Bluetooth behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en")
class CarBluetoothRadioStartTest {
    private val hop = RecordingHop()

    @Before fun thisBuildHasThisHop() = CarBluetoothHops.override(hop)

    @After fun giveTheBuildItsOwnAnswerBack() = CarBluetoothHops.resetOverride()

    @Test fun openingTheAppDoesNotStartTheCarsBluetoothHop() {
        openTheApp()

        assertFalse(
            "wired-only startup must not ask the Bluetooth hop to switch the radio on",
            hop.radioAsked.await(250, TimeUnit.MILLISECONDS),
        )
    }

    private fun openTheApp(): DiPlayActivity =
        Robolectric.buildActivity(
            DiPlayActivity::class.java,
            Intent(RuntimeEnvironment.getApplication(), DiPlayActivity::class.java),
        ).setup().get()

    /** A hop that records the one call this test is about and answers nothing else. */
    private class RecordingHop : CarBluetoothHop {
        val radioAsked = CountDownLatch(1)

        override fun applies(context: Context): Boolean = true

        override fun ensureRadioOn(context: Context) {
            radioAsked.countDown()
        }

        override fun ready(context: Context): Boolean = true

        override fun connectedPhones(context: Context): List<CarBluetoothPhone> = emptyList()

        override fun note(context: Context): String? = null

        override fun status(context: Context): String? = null

        override fun maintenance(context: Context): CarBluetoothMaintenance? = null

        override fun performMaintenance(context: Context, id: String): String = ""

        override fun busy(context: Context): Boolean = false

        override fun idleAfterSession(context: Context) = Unit
    }
}
