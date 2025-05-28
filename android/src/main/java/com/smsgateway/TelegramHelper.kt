package com.smsgateway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import com.smsgateway.ConfigProvider
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.charset.StandardCharsets
import com.smsgateway.Constants.TAG

object TelegramHelper {
    fun send(config: ConfigProvider, payload: JSONObject) {
        val chatIds: JSONArray = config.telegramChatIds
        val token: String = config.telegramToken ?: ""
        val parseMode: String = config.telegramParseMode

        if (chatIds.length() > 0 && token.length > 0) {
            val msgBody = payload.getString("msg")
            val sender = payload.getString("sender")
            val timestamp = payload.getString("timestamp")
            val phoneNumber = payload.getString("phoneNumber")
            
            val message = createMsgTemplate(msgBody, sender, phoneNumber, timestamp)

            Log.d(TAG, "Sending message: $message")

            CoroutineScope(Dispatchers.IO).launch {
                val client = okhttp3.OkHttpClient()

                for (i in 0 until chatIds.length()) {
                    try {
                        val chatId = chatIds.getString(i) ?: ""

                        
                        if(chatId.toString().length == 0) {
                            continue // skip empty chat Ids
                        }

                        val url = "https://api.telegram.org/bot${token}/sendMessage"
                        val jsonBody = JSONObject()
                        jsonBody.put("chat_id", chatId)
                        jsonBody.put("text", message) // Already escaped
                        jsonBody.put("parse_mode", parseMode)

                        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                        val body = jsonBody.toString().toRequestBody(mediaType)

                        val request = okhttp3.Request.Builder()
                            .url(url)
                            .post(body)
                            .build()

                        val response = client.newCall(request).execute()

                        val responseBody = response.body?.string() ?: "null"
                        
                        Log.i(TAG, "Sent to Telegram chat $chatId: ${response.code} -> $responseBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "Telegram Error: ${e.localizedMessage}")
                    }   
                }
            }
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
    
    private fun createMsgTemplate(msgBody: String, sender: String, phoneNumber: String, timestamp: String): String {
        val escapedMsg = escapeHtml(msgBody)
        val escapedSender = escapeHtml(sender)
        val escapedPhone = escapeHtml(phoneNumber)
        val escapedTime = escapeHtml(timestamp)
    
        return """
<b>Date</b>: <code>$escapedTime</code>
<b>From:</b> <u><code>$escapedSender</code></u>
<b>TO:</b> <u><code>$escapedPhone</code></u>
<b>Message:</b> 
<pre>$escapedMsg</pre>
""".trimIndent()
    }
}
