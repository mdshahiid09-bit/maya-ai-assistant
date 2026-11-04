package com.maya.assistant.automation

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import com.maya.assistant.service.MayaAccessibilityService

class PhoneController(private val context: Context) {

    fun openApp(appName: String): String {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        for (app in packages) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.equals(appName, ignoreCase = true) || label.contains(appName, ignoreCase = true)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opened $label"
                }
            }
        }
        return "Could not find app named $appName"
    }

    fun makeCall(phoneNumber: String): String {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Dialing $phoneNumber"
    }

    fun sendWhatsAppMessage(phone: String, message: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening WhatsApp to send message"
        } catch (e: Exception) {
            "WhatsApp is not installed"
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Set alarm for $hour:$minute"
    }

    fun adjustVolume(increase: Boolean): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return if (increase) "Volume increased" else "Volume decreased"
    }

    fun pressHome(): String {
        val service = MayaAccessibilityService.instance ?: return "Accessibility Service is not enabled"
        service.pressHome()
        return "Pressed Home"
    }

    fun pressBack(): String {
        val service = MayaAccessibilityService.instance ?: return "Accessibility Service is not enabled"
        service.pressBack()
        return "Pressed Back"
    }

    fun openNotifications(): String {
        val service = MayaAccessibilityService.instance ?: return "Accessibility Service is not enabled"
        service.openNotifications()
        return "Opened Notifications"
    }

    fun clickScreenText(text: String): String {
        val service = MayaAccessibilityService.instance ?: return "Accessibility Service is not enabled"
        val clicked = service.clickNodeWithText(text)
        return if (clicked) "Clicked on $text" else "Could not find $text on screen"
    }

    fun searchWeb(query: String): String {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Searching for $query"
    }
}
