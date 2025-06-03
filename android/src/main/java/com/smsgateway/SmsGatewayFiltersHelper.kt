package com.smsgateway

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object SmsGatewayFiltersHelper {
    fun matchesWhitelist(value: String?, whitelist: List<String>): Boolean {
        if (whitelist.isEmpty()) return true // If no filter, allow everything
        val input = value?.lowercase() ?: return false
        return whitelist.any { input.contains(it.trim().lowercase()) }
    }
}
