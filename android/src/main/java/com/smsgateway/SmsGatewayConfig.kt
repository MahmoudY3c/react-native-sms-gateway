package com.smsgateway

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SmsGatewayConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(SmsGatewayConstants.ConfigName, Context.MODE_PRIVATE)

    val isEnabled: Boolean
        get() = prefs.getBoolean("sms_listener_enabled", false)
    
    // Optional: user phone number to be sent with the data the user should insert it if needed
    val userPhoneNumber: String
        get() = prefs.getString("phoneNumber", "") ?: ""

    // can be 'http', 'telegram', 'all' default to 'all'
    val deliveryType: String
        get() = prefs.getString("delivery_type", "all") ?: "all"

    val httpConfigs: JSONArray
        get() = JSONArray(prefs.getString("http_configs", "[]") ?: "[]")

    val telegramToken: String?
        get() = prefs.getString("telegram_bot_token", null)

    val telegramChatIds: JSONArray
        get() = JSONArray(prefs.getString("telegram_chat_ids", "[]") ?: "[]")

    val telegramParseMode: String
        get() = prefs.getString("telegram_parse_mode", "HTML") ?: "HTML"

    val msgKeywordsFilterList: List<String>
        get() = (prefs.getStringSet("msg_keywords_filter_list", emptySet()) as? Set<String>)?.toList() ?: emptyList()
    
    val sendersFilterList: List<String>
        get() = (prefs.getStringSet("senders_filter_list", emptySet()) as? Set<String>)?.toList() ?: emptyList()
    
    fun getAll(): JSONObject {
        return JSONObject().apply {
            put("enabled", isEnabled)
            put("delivery_type", deliveryType)
            put("http_configs", httpConfigs)
            put("telegram_bot_token", telegramToken)
            put("telegram_chat_ids", telegramChatIds)
            put("telegram_parse_mode", telegramParseMode)
            put("msg_keywords_filter_list", msgKeywordsFilterList)
            put("senders_filter_list", sendersFilterList)
        }
    }
}
