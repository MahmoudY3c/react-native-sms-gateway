package com.smsgateway

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.smsgateway.Constants.TAG

object HttpHelper {
    fun send(configs: JSONArray, payload: JSONObject) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val mediaType = "application/json".toMediaTypeOrNull()
            val body = payload.toString().toRequestBody(mediaType)

            for (i in 0 until configs.length()) {
                try {
                    val cfg = configs.getJSONObject(i)
                    val url = cfg.getString("url") ?: ""
                    val headers = cfg.optJSONObject("headers") ?: JSONObject()

                    if(url.length == 0) {
                        continue // skip empty urls
                    }

                    val builder = Request.Builder().url(url).post(body)
                    headers.keys().forEach { key ->
                        builder.addHeader(key, headers.getString(key))
                    }

                    val response = client.newCall(builder.build()).execute()
                    Log.i(TAG, "Sent to $url: ${response.code}")
                } catch (e: Exception) {
                    Log.e(TAG, "Http Error: ${e.localizedMessage}")
                }
            }
        }
    }
}
