package com.dantesting.peakflow_sdk_testing.main

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.View
import com.dantesting.peakflow_sdk_testing.R
import com.dantesting.peakflow_sdk_testing.utils.ValueObservable
import com.synthnet.spf.MicrophoneSignalProcess
import com.synthnet.spf.SPFMode
import com.synthnet.spf.SignalProcess.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.internal.Intrinsics

class MainViewModel(private val activity: TestActivity) : OnPeakFound,
    OnAudioProcess, OnCalibrated, OnModeChanged {

    lateinit var audioManager: AudioManager

    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale("ru"))
    
    private val spfClient: MicrophoneSignalProcess
        get() = activity.spfClient

    var deviceConnected: ValueObservable<Boolean> = ValueObservable(false)
    var deviceName: ValueObservable<String> = ValueObservable("")
    var deviceType: ValueObservable<ConnectionTypes> = ValueObservable(ConnectionTypes.NONE)

    var isProcessing: ValueObservable<Boolean> = ValueObservable(false)
    var peakTimestampMessage: ValueObservable<String> = ValueObservable("")
    var frequency: ValueObservable<String> = ValueObservable("")
    var statusMessage: ValueObservable<String> = ValueObservable("Connect the device via mini-jack or bluetooth to start measuring.")

    var calibrated: ValueObservable<Boolean> = ValueObservable(false)

    private fun fanStoppedRotating(previousMode: SPFMode, mode: SPFMode): Boolean {
        return previousMode == SPFMode.MODE_TRACKING && mode == SPFMode.MODE_LISTENING
    }

    private fun blowStarted(mode: SPFMode): Boolean {
        return mode == SPFMode.MODE_UP
    }

    override fun onModeChanged(previousMode: SPFMode, mode: SPFMode) {
        Log.e("SPF mode changed", "previousMode: $previousMode -> mode: $mode")
        if (fanStoppedRotating(previousMode, mode)) {
            statusMessage.value = activity.resources.getString(R.string.TXT_MEASURED)
            spfClient.stopAnalyze(false)
        }
        if (blowStarted(mode)) {
            statusMessage.value = activity.resources.getString(R.string.TXT_MEASURING)
        }
    }

    fun onStartClick(v: View?) {
        Intrinsics.checkParameterIsNotNull(v, "v")
        val instance = spfClient
        Intrinsics.checkExpressionValueIsNotNull(instance, "spfClient")
        if (instance.isProcesing) {
            stop()
        } else {
            start()
        }
    }

    private fun start() {
        Intrinsics.checkExpressionValueIsNotNull(spfClient, "spfClient")
        if (!deviceConnected.value) {
            statusMessage.value = activity.resources.getString(R.string.TXT_NOT_CONNECTED_YET)
            return
        }
        if (!spfClient.isProcesing) {
            if (deviceType.value == ConnectionTypes.BLE) {
                audioManager.stopBluetoothSco()
                audioManager.startBluetoothSco()
            } else {
                spfClient.startCalibration(this)
                statusMessage.value = activity.resources.getString(R.string.TXT_CALIBRATING)
                frequency.value = activity.resources.getString(R.string.TXT_PEAK_FLOW_NOT_MEASURED)
                isProcessing.value = true
            }
        }
    }

    fun onScoConnected() {
        if (!deviceConnected.value) {
            statusMessage.value = activity.resources.getString(R.string.TXT_NOT_CONNECTED_YET)
            return
        }
        if (!spfClient.isProcesing) {
            spfClient.startCalibration(this)
            statusMessage.value = activity.resources.getString(R.string.TXT_CALIBRATING)
            frequency.value = activity.resources.getString(R.string.TXT_PEAK_FLOW_NOT_MEASURED)
            isProcessing.value = true
        }
    }

    fun onScoError() {
        Log.e("ScoError", "")
    }

    private fun stop() {
        if (spfClient.isProcesing) {
            if (deviceType.value == ConnectionTypes.BLE) {
                audioManager.stopBluetoothSco()
            }
            spfClient.stopAnalyze()
            statusMessage.value = if (deviceConnected.value) activity.resources.getString(R.string.TXT_CONNECTED)
                else activity.resources.getString(R.string.TXT_NOT_CONNECTED)
            calibrated.value = false
            isProcessing.value = false
        }
    }

    private fun changeDeviceConnected(deviceConnected2: Boolean) {
        deviceConnected.value = deviceConnected2
        if (deviceConnected2) {
            frequency.value = activity.resources.getString(R.string.TXT_PEAK_FLOW_NOT_MEASURED)
        } else {
            stop()
        }
    }

    override fun onResult(freq: Int) {
        frequency.value = "$freq l/min"
        peakTimestampMessage.value = simpleDateFormat.format(java.lang.Long.valueOf(Date().time))
        statusMessage.value = activity.resources.getString(R.string.TXT_WAIT_UNTIL_FAN_STOPS)
    }

    override fun onCalibrated(result: Int) {
        statusMessage.value = activity.resources.getString(R.string.TXT_WAITING_FOR_PEAKS)
        frequency.value = activity.resources.getString(R.string.TXT_PEAK_FLOW_NOT_MEASURED)
        calibrated.value = true
        spfClient.stopCalibration(false)
        spfClient.startAnalyze(this, this)
    }

    override fun onProcessFinished(peak: IntArray) {
        if (peak == null) {
            Intrinsics.throwNpe()
        }
        frequency.value = "$peak l/min"
        calibrated.value = false
        isProcessing.value = false
    }

    fun onReceive(intent: Intent, device: UsbDevice?) {
        if (deviceType.value == ConnectionTypes.NONE || deviceType.value == ConnectionTypes.WIRE) {
            val state: Int = intent.getIntExtra("state", -1)
            if (intent.hasExtra("state")) {
                if (deviceConnected.value && state == 0) {
                    changeDeviceConnected(false)
                    deviceType.value = ConnectionTypes.NONE
                    if (isProcessing.value) {
                        stop()
                    }
                } else if (!deviceConnected.value && state == 1) {
                    changeDeviceConnected(true)
                    deviceType.value = ConnectionTypes.WIRE
                }
            }
            val sb = StringBuilder()
            sb.append("state: ")
            sb.append(state)
            deviceName.value = sb.toString()
        }
    }

    fun onReceive(state: Boolean, previousState: Boolean, device: AudioDeviceInfo) {
        if (deviceType.value == ConnectionTypes.NONE || deviceType.value == ConnectionTypes.BLE) {
            if (state && !previousState) {
                changeDeviceConnected(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    deviceName.value = "${device.productName}, ${device.id}"
                }
                deviceType.value = ConnectionTypes.BLE
            } else if (!state && previousState && !deviceName.value.contains("state")) {
                changeDeviceConnected(false)
                deviceName.value = ""
                deviceType.value = ConnectionTypes.NONE
            }
        }
    }

    fun onReceive(state: Boolean, previousState: Boolean, device: BluetoothDevice) {
        if (deviceType.value == ConnectionTypes.NONE || deviceType.value == ConnectionTypes.BLE) {
            if (state && !previousState) {
                changeDeviceConnected(true)
                deviceName.value = "${device.name}, ${device.address}"
                deviceType.value = ConnectionTypes.BLE
            } else if (!state && previousState && !deviceName.value.contains("state")) {
                changeDeviceConnected(false)
                deviceName.value = ""
                deviceType.value = ConnectionTypes.NONE
            }
        }
    }

    fun appStop() {
        stop()
    }

    enum class ConnectionTypes {
        WIRE, BLE, NONE
    }
}