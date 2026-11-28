package com.shilapi.xcertplay

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.ui.component.ConfirmSheetView
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "zh-rCN")
class BluetoothGovernanceUiTest {

    @After
    fun tearDown() {
        CarPlayBackgroundSession.clear()
    }

    private fun activity(page: String = "settings"): DiPlayActivity =
        Robolectric.buildActivity(
            DiPlayActivity::class.java,
            Intent().putExtra("page", page).putExtra(DiPlayActivity.EXTRA_SETTINGS_TAB, "governance"),
        ).setup().get()

    private fun allViews(view: View): List<View> = when (view) {
        is ViewGroup -> (0 until view.childCount).map { view.getChildAt(it) }.flatMap { allViews(it) } + view
        else -> listOf(view)
    }

    private fun setSessionLive(live: Boolean) {
        val field = CarPlayBackgroundSession::class.java.getDeclaredField("stopAction")
        field.isAccessible = true
        if (live) {
            val dummyAction: (Boolean, () -> Unit) -> Unit = { _, cb -> cb() }
            field.set(CarPlayBackgroundSession, dummyAction)
        } else {
            field.set(CarPlayBackgroundSession, null)
        }
    }

    @Test
    fun `governance page renders car bluetooth restart button`() {
        val activity = activity("settings")
        val buttons = allViews(activity.findViewById(android.R.id.content)).filterIsInstance<Button>()
        val restartBtn = buttons.firstOrNull { it.text.toString() == activity.getString(R.string.bluetooth_restart_action) }

        assertNotNull("Bluetooth restart button must be present in vehicle governance", restartBtn)
        assertTrue("Bluetooth restart button must be enabled when no session is live", restartBtn!!.isEnabled)
    }

    @Test
    fun `bluetooth restart button is disabled when carplay session is active`() {
        setSessionLive(true)
        val activity = activity("settings")
        val buttons = allViews(activity.findViewById(android.R.id.content)).filterIsInstance<Button>()
        val restartBtn = buttons.firstOrNull { it.text.toString() == activity.getString(R.string.bluetooth_restart_action) }

        assertNotNull("Bluetooth restart button must be present", restartBtn)
        assertFalse("Bluetooth restart button must be disabled during active CarPlay session", restartBtn!!.isEnabled)
    }

    @Test
    fun `clicking bluetooth restart button shows confirmation sheet`() {
        val activity = activity("settings")
        val buttons = allViews(activity.findViewById(android.R.id.content)).filterIsInstance<Button>()
        val restartBtn = buttons.first { it.text.toString() == activity.getString(R.string.bluetooth_restart_action) }

        restartBtn.performClick()

        val sheet = allViews(activity.findViewById(android.R.id.content))
            .filterIsInstance<ConfirmSheetView>()
            .firstOrNull()
        assertNotNull("Confirmation sheet must be shown on click", sheet)
        val lines = allViews(sheet!!).filterIsInstance<TextView>().map { it.text.toString() }
        assertTrue(
            "The sheet must say the current Bluetooth links drop",
            lines.any { it.contains("断开") },
        )
    }

    @Test
    fun `wired-only home page hides bluetooth restart row`() {
        val activity = activity("home")
        val action = allViews(activity.findViewById(android.R.id.content))
            .filterIsInstance<TextView>()
            .firstOrNull { it.text.toString() == activity.getString(R.string.bluetooth_restart_short) }

        assertTrue("Bluetooth maintenance is not a home quick action in wired-only mode", action == null)
    }

    @Test
    fun `wired-only home keeps bluetooth restart row hidden during a session`() {
        setSessionLive(true)
        val activity = activity("home")
        val action = allViews(activity.findViewById(android.R.id.content))
            .filterIsInstance<TextView>()
            .firstOrNull { it.text.toString() == activity.getString(R.string.bluetooth_restart_short) }

        assertTrue("Bluetooth maintenance remains hidden on the wired-only home", action == null)
    }
}
