package com.dantesting.peakflow_sdk_testing.connect_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dantesting.peakflow_sdk_testing.main.TestActivity


class JackReceiver internal constructor(private val activity: TestActivity) :
    BroadcastReceiver() {

    override fun onReceive(p0: Context?, intent: Intent?) {
        if (intent != null) {
            if (intent.extras!!.getInt("microphone", -1) == 1)
                activity.mainViewModel.onReceive(intent, null)
        }
    }

}