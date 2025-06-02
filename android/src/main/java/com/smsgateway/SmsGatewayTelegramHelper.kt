package com.smsgateway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import com.smsgateway.SmsGatewayConfig
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.charset.StandardCharsets
import com.smsgateway.SmsGatewayConstants.TAG

object SmsGatewayTelegramHelper {
    fun send(config: SmsGatewayConfig, payload: JSONObject) {
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

                        // val url = "https://api.telegram.org/bot${token}/sendMessage" +
                        //           "?chat_id=$chatId&text=$message&parse_mode=$parseMode"

                        // val request = okhttp3.Request.Builder().url(url).get().build()
                        // val response = client.newCall(request).execute()
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

    private fun escapeMarkdownV2(text: String): String {
        val regex = Regex("""([_*\[\]()~`>#+\-=|{}.!])""")
        return text.replace(regex) { "\\${it.value}" }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
    
    fun encodeURIComponent(input: String): String {
        val result = StringBuilder()
        for (c in input) {
            if (isSafeChar(c)) {
                result.append(c)
            } else {
                val bytes = c.toString().toByteArray(StandardCharsets.UTF_8)
                for (b in bytes) {
                    result.append('%')
                    result.append(String.format("%02X", b.toInt() and 0xFF))
                }
            }
        }
        return result.toString()
    }

    private fun isSafeChar(c: Char): Boolean {
        return when (c) {
            in 'a'..'z', in 'A'..'Z', in '0'..'9' -> true
            '-', '_', '.', '!', '~', '*', '\'', '(', ')' -> true
            else -> false
        }
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
