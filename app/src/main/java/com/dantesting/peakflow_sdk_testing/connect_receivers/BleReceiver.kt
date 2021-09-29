package com.dantesting.peakflow_sdk_testing.connect_receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.util.Log
import com.dantesting.peakflow_sdk_testing.main.TestActivity
import java.util.HashMap

class BleReceiver internal constructor(private val activity: TestActivity) :
    BroadcastReceiver() {

    override fun onReceive(p0: Context?, intent: Intent?) {
        if (intent != null) {
            val hash: HashMap<String, String> = HashMap()
            for (r in intent.extras!!.keySet())
                hash[r] = intent.extras!!.get(r).toString()
            when (intent.action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    Log.e("CONNECTION_CHANGED", hash.toString())
                    activity.mainViewModel.onReceive(
                        intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0) == 2,
                        intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 0) == 2,
                        intent.extras!!.get("android.bluetooth.device.extra.DEVICE") as BluetoothDevice
                    )
                }
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    Log.e("SCO_AUDIO", hash.toString())
                    val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 0)
                    when (state) {
                        AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                            activity.mainViewModel.onScoConnected()
                        }
                        AudioManager.SCO_AUDIO_STATE_ERROR -> {
                            activity.mainViewModel.onScoError()
                        }
                    }
                }
            }
        }
    }

}