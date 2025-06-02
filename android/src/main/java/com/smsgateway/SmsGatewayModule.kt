package com.smsgateway

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import android.content.SharedPreferences
import com.facebook.react.bridge.*
import com.smsgateway.SmsGatewayConstants

class SmsGatewayModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {
  private val prefs: SharedPreferences = reactContext.getSharedPreferences(SmsGatewayConstants.ConfigName, Context.MODE_PRIVATE)

  override fun getName(): String {
    return SmsGatewayConstants.NAME
  }

  @ReactMethod fun enableSmsListener(enabled: Boolean) {
      prefs.edit().putBoolean("sms_listener_enabled", enabled).apply()
  }
  
  @ReactMethod fun setHttpConfigs(configsJson: String) {
      prefs.edit().apply {
          putString("http_configs", configsJson)
      }.apply()
  }
  
  @ReactMethod fun setSendersFilterList(list: ReadableArray) {
      val editor = prefs.edit()
      val set = mutableSetOf<String>()
      for (i in 0 until list.size()) {
          set.add(list.getString(i) ?: continue)
      }
      editor.putStringSet("senders_filter_list", set).apply()
  }
  
  @ReactMethod fun getSendersFilterList(promise: Promise) {
      val set = prefs.getStringSet("senders_filter_list", emptySet()) ?: emptySet()
      val array = Arguments.createArray()
      set.forEach { array.pushString(it) }
      promise.resolve(array)
  }
  
  
  @ReactMethod fun setMsgKeywordsFilterList(list: ReadableArray) {
      val editor = prefs.edit()
      val set = mutableSetOf<String>()
      for (i in 0 until list.size()) {
          set.add(list.getString(i) ?: continue)
      }
      editor.putStringSet("msg_keywords_filter_list", set).apply()
  }
  
  @ReactMethod fun getMsgKeywordsFilterList(promise: Promise) {
      val set = prefs.getStringSet("msg_keywords_filter_list", emptySet()) ?: emptySet()
      val array = Arguments.createArray()
      set.forEach { array.pushString(it) }
      promise.resolve(array)
  }
  
  @ReactMethod fun setTelegramConfig(configJson: String) {
      val json = JSONObject(configJson)
      prefs.edit().apply {
          putString("telegram_bot_token", json.optString("bot_token"))
          putString("telegram_chat_ids", json.optJSONArray("chat_ids")?.toString() ?: "[]")
      }.apply()
  }
  
  @ReactMethod fun setTelegramBotToken(bot_token: String) {
      prefs.edit().putString("telegram_bot_token", bot_token).apply()
  }
  
  @ReactMethod fun setTelegramChatIds(chat_ids: String) {
      val json = JSONArray(chat_ids)
      prefs.edit().putString("telegram_chat_ids", json.toString() ?: "[]").apply()
  }
  
  @ReactMethod fun setDeliveryType(delivery_type: String) {
      prefs.edit().putString("delivery_type", delivery_type).apply()
  }
  
  @ReactMethod fun getHttpConfigs(promise: Promise) {
      promise.resolve(prefs.getString("http_configs", "[]"))
  }
  
  @ReactMethod fun isSmsListenerEnabled(promise: Promise) {
      promise.resolve(prefs.getBoolean("sms_listener_enabled", false))
  }
  
  @ReactMethod
  fun getTelegramBotToken(promise: Promise) {
      promise.resolve(prefs.getString("telegram_bot_token", null))
  }
  
  @ReactMethod
  fun getTelegramChatIds(promise: Promise) {
      promise.resolve(prefs.getString("telegram_chat_ids", "[]"))
  }
  
  @ReactMethod
  fun getTelegramParseMode(promise: Promise) {
      promise.resolve(prefs.getString("telegram_parse_mode", "HTML"))
  }
  
  @ReactMethod
  fun getUserPhoneNumber(promise: Promise) {
      promise.resolve(prefs.getString("phoneNumber", ""))
  }
  
  @ReactMethod
  fun setUserPhoneNumber(userPhoneNumber: String) {
      prefs.edit().putString("phoneNumber", userPhoneNumber).apply()
  }
  
  @ReactMethod
  fun getDeliveryType(promise: Promise) {
      promise.resolve(prefs.getString("delivery_type", "all"))
  }
  
  @ReactMethod
  fun getAllSettings(promise: Promise) {
      try {
          val result = Arguments.createMap()
          result.putBoolean("sms_listener_enabled", prefs.getBoolean("sms_listener_enabled", false))
          result.putString("delivery_type", prefs.getString("delivery_type", "all"))
          result.putString("phoneNumber", prefs.getString("phoneNumber", ""))
          result.putString("telegram_bot_token", prefs.getString("telegram_bot_token", null))
          result.putString("telegram_parse_mode", prefs.getString("telegram_parse_mode", "HTML"))
          
          // http_configs: Array<{ url: string; headers?: Record<string, string> }>
          val httpConfigsArray = Arguments.createArray()
          val httpConfigsJsonStr = prefs.getString("http_configs", "[]") ?: "[]"
          val httpConfigsJson = JSONArray(httpConfigsJsonStr)
          for (i in 0 until httpConfigsJson.length()) {
              val configObj = httpConfigsJson.getJSONObject(i)
              val configMap = Arguments.createMap()
              // Required: url
              configMap.putString("url", configObj.optString("url", ""))
              // Optional: headers object
              if (configObj.has("headers")) {
                  val headersObj = configObj.optJSONObject("headers")
                  val headersMap = Arguments.createMap()
                  if (headersObj != null) {
                      headersObj.keys().forEach { key ->
                          headersMap.putString(key, headersObj.getString(key))
                      }
                      configMap.putMap("headers", headersMap)
                  }
              }
              httpConfigsArray.pushMap(configMap)
          }
          
          result.putArray("http_configs", httpConfigsArray)
          // telegram_chat_ids: Array<string | number>
          val chatIdsArray = Arguments.createArray()
          val chatIdsStr = prefs.getString("telegram_chat_ids", "[]") ?: "[]"
          val chatIdsJson = JSONArray(chatIdsStr)
          for (i in 0 until chatIdsJson.length()) {
              when (val id = chatIdsJson.get(i)) {
                  is String -> chatIdsArray.pushString(id)
                  is Int -> chatIdsArray.pushInt(id)
                  is Long -> chatIdsArray.pushDouble(id.toDouble()) // React Native doesn't support Long
                  is Double -> chatIdsArray.pushDouble(id)
                  else -> chatIdsArray.pushString(id.toString()) // Fallback
              }
          }
          result.putArray("telegram_chat_ids", chatIdsArray)
          
          val keywordsFilterListSet = prefs.getStringSet("msg_keywords_filter_list", emptySet()) ?: emptySet()
          val keywordsFilterListArray = Arguments.createArray()
          keywordsFilterListSet.forEach { keywordsFilterListArray.pushString(it) }
          result.putArray("msg_keywords_filter_list", keywordsFilterListArray)
          
          val sendersFilterListSet = prefs.getStringSet("senders_filter_list", emptySet()) ?: emptySet()
          val sendersFilterListArray = Arguments.createArray()
          sendersFilterListSet.forEach { sendersFilterListArray.pushString(it) }
          result.putArray("senders_filter_list", sendersFilterListArray)
          promise.resolve(result)
      } catch (e: Exception) {
          promise.reject("ERROR_GETTING_SETTINGS", e)
      }
  }
}
