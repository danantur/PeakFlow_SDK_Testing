package com.dantesting.peakflow_sdk_testing.main

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dantesting.peakflow_sdk_testing.R
import com.dantesting.peakflow_sdk_testing.connect_receivers.BleReceiver
import com.dantesting.peakflow_sdk_testing.connect_receivers.JackReceiver
import com.dantesting.peakflow_sdk_testing.connect_receivers.UsbReceiver
import com.synthnet.spf.MicrophoneSignalProcess
import io.reactivex.rxjava3.disposables.Disposable
import kotlin.jvm.internal.Intrinsics


class TestActivity : AppCompatActivity() {

    val spfClient: MicrophoneSignalProcess = MicrophoneSignalProcess.getInstance()
    val mainViewModel: MainViewModel = MainViewModel(this)

    private val jackReceiver: JackReceiver = JackReceiver(this)
    private val usbReceiver: UsbReceiver = UsbReceiver(this)
    private val bleReceiver: BleReceiver = BleReceiver(this)

    lateinit var manager: UsbManager

    private lateinit var btnStart: Button
    private lateinit var text: TextView
    private lateinit var frequency: TextView

    private val disposables: ArrayList<Disposable> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val systemService = getSystemService(Context.USB_SERVICE)
        if (systemService != null) {
            manager = systemService as UsbManager
        }

        if (!(ContextCompat.checkSelfPermission(
                this,
                "android.permission.RECORD_AUDIO"
            ) == 0 && ContextCompat.checkSelfPermission(
                this,
                "android.permission.WRITE_EXTERNAL_STORAGE"
            ) == 0) && Build.VERSION.SDK_INT >= 23)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    "android.permission.RECORD_AUDIO",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
                ), 42
            )

        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mainViewModel.audioManager = audioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devicesTypes: ArrayList<Int> = ArrayList()
            for (r in audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS))
                devicesTypes.add(r.type)
            if (AudioDeviceInfo.TYPE_BLUETOOTH_SCO in devicesTypes) {
                mainViewModel.onReceive(true, false,
                    audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).first { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } )
            }
            else if (AudioDeviceInfo.TYPE_WIRED_HEADSET in devicesTypes) {
                mainViewModel.onReceive(Intent().putExtra("state", 1), null )
            }
        }
        else {
            mainViewModel.deviceConnected.value = audioManager.isWiredHeadsetOn || audioManager.isBluetoothScoOn
            if (audioManager.isWiredHeadsetOn) {
                mainViewModel.deviceType.value = MainViewModel.ConnectionTypes.WIRE
            } else if (audioManager.isBluetoothScoOn)
                mainViewModel.deviceType.value = MainViewModel.ConnectionTypes.BLE
        }

        btnStart = findViewById(R.id.start)
        text = findViewById(R.id.textView)
        frequency = findViewById(R.id.textView2)

        btnStart.setOnClickListener { mainViewModel.onStartClick(it) }
    }

    override fun onResume() {
        super.onResume()
        startChecking()
        val intentFilter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        registerReceiver(bleReceiver, intentFilter)
        registerReceiver(jackReceiver, IntentFilter(AudioManager.ACTION_HEADSET_PLUG))
        registerReceiver(
            usbReceiver,
            IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED")
        )
        registerReceiver(
            usbReceiver,
            IntentFilter("android.hardware.usb.action.USB_DEVICE_DETACHED")
        )
    }

    override fun onPause() {
        stopChecking()
        unregisterReceiver(bleReceiver)
        unregisterReceiver(jackReceiver)
        unregisterReceiver(usbReceiver)
        mainViewModel.appStop()
        super.onPause()
    }

    override fun onDestroy() {
        mainViewModel.appStop()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Intrinsics.checkParameterIsNotNull(permissions, "permissions")
        Intrinsics.checkParameterIsNotNull(grantResults, "grantResults")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 42) {
            return
        }
        if (ContextCompat.checkSelfPermission(
                this,
                "android.permission.RECORD_AUDIO"
            ) != 0 || ContextCompat.checkSelfPermission(
                this,
                "android.permission.WRITE_EXTERNAL_STORAGE"
            ) != 0
        ) {
            finish()
        }
    }


    private fun startChecking() {
        disposables.addAll(
            arrayListOf(
                mainViewModel.deviceConnected.subscribe({
                    if (it) text.text = resources.getString(R.string.TXT_CONNECTED)
                    else text.text = resources.getString(R.string.TXT_NOT_CONNECTED)
                }, logErrorLambda("deviceConnected")),
                mainViewModel.deviceName.subscribe({ title = it }, logErrorLambda("deviceName")),
                mainViewModel.calibrated.subscribe(logLambda("calibrated"), logErrorLambda("calibrated")),
                mainViewModel.isProcessing.subscribe(logLambda("isProcessing"), logErrorLambda("isProcessing")),
                mainViewModel.peakTimestampMessage.subscribe(logLambda("peakTimestampMessage"), logErrorLambda("frequency")),
                mainViewModel.frequency.subscribe({ frequency.text = it }, logErrorLambda("frequency")),
                mainViewModel.statusMessage.subscribe({ text.text = it }, logErrorLambda("statusMessage")),
                mainViewModel.deviceType.subscribe(logLambda("deviceType"), logErrorLambda("deviceType"))
            )
        )
    }

    private fun stopChecking() {
        disposables.forEach {
            it.dispose()
        }
        disposables.clear()
    }

    private fun logLambda(tag: String): (Any) -> Unit = { Log.e(tag, it.toString()) }
    private fun logErrorLambda(tag: String): (Throwable) -> Unit = { it.printStackTrace() }

    private fun makeToast(msg: String) {
        Toast.makeText(
            applicationContext,
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }
}