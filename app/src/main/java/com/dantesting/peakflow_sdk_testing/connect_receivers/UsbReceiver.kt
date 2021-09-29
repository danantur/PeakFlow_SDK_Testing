package com.dantesting.peakflow_sdk_testing.connect_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.dantesting.peakflow_sdk_testing.main.TestActivity
import kotlin.jvm.internal.Intrinsics


class UsbReceiver internal constructor(private val activity: TestActivity) :
    BroadcastReceiver() {

    override fun onReceive(p0: Context?, intent: Intent) {
        lateinit var deviceList: HashMap<String?, UsbDevice?>
        lateinit var values: Collection<UsbDevice?>
        Intrinsics.checkParameterIsNotNull(p0, "p0")
        Intrinsics.checkParameterIsNotNull(intent, "intent")
        var device: UsbDevice? = null
        if (Intrinsics.areEqual(intent.action, "android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
            val manager: UsbManager = activity.manager
            if (!(manager.deviceList.also {
                    deviceList = it
                } == null || deviceList.values.also {
                    values = it
                } == null)) {
                device = values.first()
            }
            intent.putExtra("state", 1)
        }
        activity.mainViewModel.onReceive(intent, device)
    }
}