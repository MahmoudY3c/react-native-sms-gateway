package com.smsgateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.widget.Toast
import android.util.Log
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionInfo
import android.telephony.SmsMessage
import android.os.Bundle

import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule

class SmsGatewayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val config = SmsGatewayConfig(context)
        if (!config.isEnabled) return

        val smsMessages: Array<SmsMessage> =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } else {
            val bundle: Bundle? = intent.extras
            val pdus = bundle?.get("pdus") as? Array<*>
            pdus?.map {
                SmsMessage.createFromPdu(it as ByteArray)
            }?.toTypedArray() ?: return
        }


        val firstMessage = smsMessages.firstOrNull() ?: return
        val messageBody = smsMessages.joinToString("\n") { it.messageBody }
        val sender = firstMessage?.originatingAddress ?: "unknown"
        val timestamp = firstMessage?.timestampMillis ?: System.currentTimeMillis()
        val phoneNumber = config.userPhoneNumber ?: ""

        val senderWhitelist = config.sendersFilterList
        val msgKeywordsWhiteList = config.msgKeywordsFilterList

        val isSenderMatchFilter = SmsGatewayFiltersHelper.matchesWhitelist(sender, senderWhitelist)
        val isMsgMatchKeywords = SmsGatewayFiltersHelper.matchesWhitelist(messageBody, msgKeywordsWhiteList)

        
        // Only send if either matches
        if (!isSenderMatchFilter && !isMsgMatchKeywords) {
            Log.i(SmsGatewayConstants.TAG, "Sender and message do not match any whitelist items. Ignoring.")
            return
        }

        // val intentExtras = intent.extras
        // // ✅ Get SIM slot from intent extras (may vary by OEM)
        // val simSlot = intentExtras.getInt("slot", -1) // Some devices use "simSlot", "simId", or "subscription"

        val subscriptionId = intent.getIntExtra("subscription", -1)
        val simSlot = getSimSlotFromSubscriptionId(context, subscriptionId)

        Log.d(SmsGatewayConstants.TAG, "SMS from: $sender\nMessage: $messageBody\nSlot: $simSlot\nPhoneNumber: $phoneNumber")
        

        Toast.makeText(context, "SMS received from $sender", Toast.LENGTH_LONG).show()

        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        val payload = JSONObject().apply {
            put("sender", sender)
            put("msg", messageBody)
            put("timestamp", formattedTime)
            put("phoneNumber", phoneNumber)
        }

        // NotificationHelper(context).showNotification("SMS received", "From: $sender\n$messageBody")

        // emit event to js so it can show received SMS message and start work with it
        sendToJs(context, payload)

        // ✅ Dispatch based on deliveryType
        when (config.deliveryType.lowercase()) {
            "http" -> SmsGatewayHttpHelper.send(config.httpConfigs, payload)
            "telegram" -> SmsGatewayTelegramHelper.send(config, payload)
            "all" -> {
                // Send to both by default if deliveryType is unknown
                SmsGatewayHttpHelper.send(config.httpConfigs, payload)
                SmsGatewayTelegramHelper.send(config, payload)
            }
            else -> Log.e(SmsGatewayConstants.TAG, "Unknown deliveryType `${config.deliveryType}`")
        }
    }

    private fun getSimSlotFromSubscriptionId(context: Context, subId: Int): Int {
        if (subId == -1) return -1
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val infoList = subscriptionManager.activeSubscriptionInfoList
            infoList?.firstOrNull { it.subscriptionId == subId }?.simSlotIndex ?: -1
        } catch (e: Exception) {
            Log.e(SmsGatewayConstants.TAG, "Error getting simSlot: ${e.message}")
            -1
        }
    }

    private fun sendToJs(context: Context, payload: JSONObject) {
        val app = context.applicationContext as? ReactApplication ?: return
        val msgBody = payload.getString("msg")
        val sender = payload.getString("sender")
        val timestamp = payload.getString("timestamp")
        val phoneNumber = payload.getString("phoneNumber")
        val reactContext = app.reactNativeHost.reactInstanceManager.currentReactContext

        reactContext?.let {
            val params = Arguments.createMap().apply {
                putString("msg", msgBody)
                putString("timestamp", timestamp)
                putString("phoneNumber", phoneNumber)
                putString("sender", sender)
            }
            it
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(SmsGatewayConstants.SmsEvent, params)
        }
    }
}
