package com.shilapi.xcertplay

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.shilapi.xcertplay.transport.adapter.AdapterRoutes
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Regression coverage for the E01 single-user, USB-wired product profile.
 * Wireless setup is intentionally absent; the remaining settings and navigation stay usable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en")
class SettingsPagesUiTest {
    @Before fun prepare() {
        AdapterRoutes.override(null)
        ExperimentalFeatures.override(true)
    }

    @After fun cleanup() {
        AdapterRoutes.resetOverride()
        ExperimentalFeatures.resetOverride()
    }

    private fun open(page: String = "settings", tab: String? = null): DiPlayActivity {
        val intent = Intent(RuntimeEnvironment.getApplication(), DiPlayActivity::class.java)
            .putExtra("page", page)
        if (tab != null) intent.putExtra(DiPlayActivity.EXTRA_SETTINGS_TAB, tab)
        return Robolectric.buildActivity(DiPlayActivity::class.java, intent).setup().get()
    }

    private fun allViews(activity: DiPlayActivity): List<View> {
        val result = mutableListOf<View>()
        fun visit(view: View) {
            result += view
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(activity.findViewById(android.R.id.content))
        return result
    }

    private fun texts(activity: DiPlayActivity): List<String> =
        allViews(activity).filterIsInstance<TextView>().mapNotNull { it.text?.toString() }.filter { it.isNotBlank() }

    private fun selectTab(activity: DiPlayActivity, label: String) {
        val tab = allViews(activity).filterIsInstance<TextView>().first { it.text?.toString() == label }
        tab.performClick()
    }

    @Test fun connectionSettingsExposeWiredSetupAndNoWirelessControls() {
        val activity = open()
        val shown = texts(activity)
        assertTrue("wired settings remain available", "Wired settings" in shown)
        assertFalse("wireless settings are not exposed", "Wireless settings" in shown)
        assertFalse("hotspot setup is not exposed", shown.any { it.contains("Car hotspot") || it.contains("hotspot settings") })
        assertFalse("Bluetooth pairing is not exposed", shown.any { it.contains("Bluetooth and phone") || it.contains("Open Bluetooth") })
    }

    @Test fun homeStageOffersUsbAndDoesNotOfferWireless() {
        val activity = open("home")
        val buttons = allViews(activity).filterIsInstance<Button>().mapNotNull { it.text?.toString() }
        assertTrue("USB connection action is present", "Wired" in buttons)
        assertFalse("wireless connection action is hidden", buttons.any { it.contains("Wireless", ignoreCase = true) })
        assertFalse("the old transport chooser is gone", buttons.any { it.contains("Choose connection mode") })
    }

    @Test fun settingsNavigationKeepsAudioDisplayGovernanceAndSystemTabs() {
        val activity = open()
        val tabs = texts(activity)
        listOf("Connection", "Audio routing", "Display and performance", "Vehicle governance", "System settings")
            .forEach { assertTrue("settings tab '$it' remains available", it in tabs) }

        selectTab(activity, "Audio routing")
        assertTrue("audio controls remain accessible", texts(activity).any { it.contains("Audio routing") || it.contains("Navigation stream type") })

        selectTab(activity, "Display and performance")
        assertTrue("display controls remain accessible", texts(activity).any { it.contains("Resolution") || it.contains("Display") })

        selectTab(activity, "Vehicle governance")
        assertTrue("vehicle governance controls remain accessible", "Vehicle governance" in texts(activity))

        selectTab(activity, "System settings")
        assertTrue("system controls remain accessible", "System settings" in texts(activity))
    }

    @Test fun backFromSettingsReturnsToUsbHome() {
        val activity = open()
        val back = allViews(activity).filterIsInstance<Button>().first { it.text?.toString() == "Back" }
        back.performClick()
        val buttons = allViews(activity).filterIsInstance<Button>().mapNotNull { it.text?.toString() }
        assertTrue("back returns to the USB home stage", "Wired" in buttons)
        assertFalse("settings page is no longer displayed", "System settings" in texts(activity))
        assertFalse("back does not exit the app", activity.isFinishing)
    }

    @Test fun retiredWirelessRoutesFailClosedToHome() {
        listOf("wireless", "wired").forEach { route ->
            val activity = open(route)
            val buttons = allViews(activity).filterIsInstance<Button>().mapNotNull { it.text?.toString() }
            assertTrue("$route route falls back to the wired home", "Wired" in buttons)
            assertFalse("$route route does not expose wireless action", buttons.any { it.contains("Wireless", ignoreCase = true) })
        }
    }

    @Test fun settingsTabsCanBeChangedWithoutDuplicatingActions() {
        val activity = open()
        listOf("Connection", "Audio routing", "Display and performance", "Vehicle governance", "System settings").forEach { tab ->
            selectTab(activity, tab)
            val labels = allViews(activity).filterIsInstance<Button>().mapNotNull { it.text?.toString() }
            val repeated = labels.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertTrue("$tab duplicates actions: $repeated", repeated.isEmpty())
        }
    }
}
