package com.shilapi.xcertplay

import android.content.Intent
import android.hardware.usb.UsbManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbAutoLaunchTest {

    @Test
    fun appleVendorIdIsRecognizedAcrossProductIdChanges() {
        // Apple keeps the vendor ID stable while product IDs vary by iPhone model and iOS version.
        assertTrue(UsbAutoLaunch.isAppleVendorId(0x05AC))
    }

    @Test
    fun nonAppleUsbDevicesDoNotTriggerCarPlayAutoLaunch() {
        assertFalse(UsbAutoLaunch.isAppleVendorId(0x18D1))
        assertFalse(UsbAutoLaunch.isAppleVendorId(0x046D))
    }

    @Test
    fun onlyUsbAttachActionCanStartTheAutomaticFlow() {
        assertFalse(UsbAutoLaunch.isAppleAttach(Intent(Intent.ACTION_MAIN)))
        assertFalse(UsbAutoLaunch.isAppleAttach(Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)))
        // An attach event without a device payload is malformed and must be ignored.
        assertFalse(UsbAutoLaunch.isAppleAttach(Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED)))
    }
}
