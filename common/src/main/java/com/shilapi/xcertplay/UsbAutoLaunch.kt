package com.shilapi.xcertplay

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

/**
 * Identifies the iPhone/iPad USB attach event that is allowed to auto-open the wired workflow.
 * Product IDs deliberately are not pinned: Apple changes them across device and iOS generations.
 */
internal object UsbAutoLaunch {
    const val APPLE_VENDOR_ID = 0x05AC
    const val EXTRA_AUTO_CONNECT = "com.shihab.diplay.EXTRA_USB_AUTO_CONNECT"

    fun isAppleVendorId(vendorId: Int): Boolean = vendorId == APPLE_VENDOR_ID

    fun isAppleDevice(device: UsbDevice?): Boolean =
        device != null && isAppleVendorId(device.vendorId)

    @Suppress("DEPRECATION")
    fun isAppleAttach(intent: Intent?): Boolean {
        if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return false
        val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
        return isAppleDevice(device)
    }
}
