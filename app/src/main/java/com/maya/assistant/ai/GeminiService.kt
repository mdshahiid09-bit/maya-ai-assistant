package com.maya.assistant.ai

import com.maya.assistant.automation.PhoneController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(
    private val apiKey: String,
    private val systemPrompt: String,
    private val phoneController: PhoneController
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .build()

    suspend fun processUserPrompt(userInput: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Please configure your Gemini API Key in Settings first."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val toolsJson = JSONArray().put(
            JSONObject().put("functionDeclarations", JSONArray()
                .put(createFunc("open_app", "Opens an installed Android application", "app_name", "string", "Name of the app to open"))
                .put(createFunc("make_call", "Dials a phone number", "phone_number", "string", "Phone number to call"))
                .put(createWhatsAppFunc())
                .put(createAlarmFunc())
                .put(createFunc("adjust_volume", "Increases or decreases device media volume", "direction", "string", "'up' or 'down'"))
                .put(createSimpleFunc("press_home", "Navigates to the phone's home screen"))
                .put(createSimpleFunc("press_back", "Performs an Android back button press"))
                .put(createSimpleFunc("open_notifications", "Pulls down the notification shade"))
                .put(createFunc("click_screen_text", "Clicks a button or text currently visible on screen", "text", "string", "Exact text to click"))
                .put(createFunc("search_web", "Performs a Google web search", "query", "string", "Search query"))
            )
        )

        val contentsJson = JSONArray().put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userInput)))
        )

        val requestBodyJson = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            .put("contents", contentsJson)
            .put("tools", toolsJson)

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext "Empty response from Gemini"
                val json = JSONObject(body)

                if (json.has("error")) {
                    return@withContext "Gemini API Error: ${json.getJSONObject("error").optString("message")}"
                }

                val candidates = json.optJSONArray("candidates") ?: return@withContext "No response from AI."
                val firstCandidate = candidates.getJSONObject(0)
                val parts = firstCandidate.getJSONObject("content").getJSONArray("parts")

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("functionCall")) {
                        val call = part.getJSONObject("functionCall")
                        val funcName = call.getString("name")
                        val args = call.optJSONObject("args") ?: JSONObject()
                        val actionResult = executeAction(funcName, args)
                        return@withContext actionResult
                    } else if (part.has("text")) {
                        return@withContext part.getString("text")
                    }
                }
                "Action processed."
            }
        } catch (e: Exception) {
            "Network error: ${e.localizedMessage}"
        }
    }

    private fun executeAction(name: String, args: JSONObject): String {
        return when (name) {
            "open_app" -> phoneController.openApp(args.optString("app_name"))
            "make_call" -> phoneController.makeCall(args.optString("phone_number"))
            "send_whatsapp" -> phoneController.sendWhatsAppMessage(args.optString("phone"), args.optString("message"))
            "set_alarm" -> phoneController.setAlarm(args.optInt("hour", 7), args.optInt("minute", 0), args.optString("label", "Alarm"))
            "adjust_volume" -> phoneController.adjustVolume(args.optString("direction").equals("up", ignoreCase = true))
            "press_home" -> phoneController.pressHome()
            "press_back" -> phoneController.pressBack()
            "open_notifications" -> phoneController.openNotifications()
            "click_screen_text" -> phoneController.clickScreenText(args.optString("text"))
            "search_web" -> phoneController.searchWeb(args.optString("query"))
            else -> "Unknown action: $name"
        }
    }

    private fun createSimpleFunc(name: String, desc: String): JSONObject {
        return JSONObject().put("name", name).put("description", desc)
    }

    private fun createFunc(name: String, desc: String, paramName: String, paramType: String, paramDesc: String): JSONObject {
        return JSONObject()
            .put("name", name)
            .put("description", desc)
            .put("parameters", JSONObject()
                .put("type", "object")
                .put("properties", JSONObject().put(paramName, JSONObject().put("type", paramType).put("description", paramDesc)))
                .put("required", JSONArray().put(paramName))
            )
    }

    private fun createWhatsAppFunc(): JSONObject {
        return JSONObject()
            .put("name", "send_whatsapp")
            .put("description", "Sends a message to a WhatsApp number")
            .put("parameters", JSONObject()
                .put("type", "object")
                .put("properties", JSONObject()
                    .put("phone", JSONObject().put("type", "string").put("description", "Phone number with country code"))
                    .put("message", JSONObject().put("type", "string").put("description", "Message text"))
                )
                .put("required", JSONArray().put("phone").put("message"))
            )
    }

    private fun createAlarmFunc(): JSONObject {
        return JSONObject()
            .put("name", "set_alarm")
            .put("description", "Sets an alarm at a specific hour and minute")
            .put("parameters", JSONObject()
                .put("type", "object")
                .put("properties", JSONObject()
                    .put("hour", JSONObject().put("type", "integer").put("description", "Hour in 24-hour format (0-23)"))
                    .put("minute", JSONObject().put("type", "integer").put("description", "Minute (0-59)"))
                    .put("label", JSONObject().put("type", "string").put("description", "Alarm label or message"))
                )
                .put("required", JSONArray().put("hour").put("minute"))
            )
    }
}
