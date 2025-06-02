package com.smsgateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smsgateway.SmsGatewayConstants.TAG

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // NotificationHelper(context).showNotification("Device boot", "service is still running after device boot")
            Log.i(TAG, "Device rebooted - consider re-initializing settings if needed")
        }
    }
}
