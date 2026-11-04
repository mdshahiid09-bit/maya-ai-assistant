package com.maya.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.maya.assistant.MayaApp
import com.maya.assistant.R
import com.maya.assistant.ai.GeminiService
import com.maya.assistant.automation.PhoneController
import com.maya.assistant.data.ChatMessage
import com.maya.assistant.service.MayaAccessibilityService
import com.maya.assistant.voice.ElevenLabsService
import com.maya.assistant.voice.SpeechRecognizerManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var phoneController: PhoneController
    private lateinit var geminiService: GeminiService
    private lateinit var elevenLabsService: ElevenLabsService
    private var speechRecognizerManager: SpeechRecognizerManager? = null

    private lateinit var statusText: TextView
    private lateinit var voiceOrb: View
    private lateinit var editTextInput: EditText
    private lateinit var accessibilityBanner: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        voiceOrb = findViewById(R.id.voiceOrb)
        editTextInput = findViewById(R.id.editTextInput)
        accessibilityBanner = findViewById(R.id.accessibilityBanner)

        val btnSettings: ImageView = findViewById(R.id.btnSettings)
        val btnSend: ImageButton = findViewById(R.id.btnSend)
        val fabMic: FloatingActionButton = findViewById(R.id.fabMic)
        val btnEnableAccess: View = findViewById(R.id.btnEnableAccessibility)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewChat)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = adapter

        phoneController = PhoneController(this)
        val prefs = MayaApp.instance.prefs
        geminiService = GeminiService(prefs.geminiApiKey, prefs.systemPrompt, phoneController)
        elevenLabsService = ElevenLabsService(this, prefs.elevenLabsApiKey, prefs.elevenLabsVoiceId)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnEnableAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnSend.setOnClickListener {
            val text = editTextInput.text.toString().trim()
            if (text.isNotEmpty()) {
                editTextInput.text.clear()
                handleUserInput(text)
            }
        }

        checkAudioPermission()

        speechRecognizerManager = SpeechRecognizerManager(
            this,
            onResult = { query -> handleUserInput(query) },
            onStatusChange = { status -> statusText.text = status }
        )

        fabMic.setOnClickListener {
            if (checkAudioPermission()) {
                speechRecognizerManager?.startListening()
            }
        }

        messages.add(ChatMessage("Hello! I am Maya, your personal AI assistant. How can I help you?", false))
        adapter.notifyItemInserted(messages.size - 1)
    }

    override fun onResume() {
        super.onResume()
        accessibilityBanner.visibility = if (MayaAccessibilityService.isRunning) View.GONE else View.VISIBLE
        val prefs = MayaApp.instance.prefs
        geminiService = GeminiService(prefs.geminiApiKey, prefs.systemPrompt, phoneController)
        elevenLabsService = ElevenLabsService(this, prefs.elevenLabsApiKey, prefs.elevenLabsVoiceId)
    }

    private fun handleUserInput(input: String) {
        messages.add(ChatMessage(input, true))
        adapter.notifyItemInserted(messages.size - 1)

        statusText.text = "Maya is thinking..."
        voiceOrb.animate().scaleX(1.3f).scaleY(1.3f).setDuration(300).start()

        lifecycleScope.launch {
            val response = geminiService.processUserPrompt(input)
            messages.add(ChatMessage(response, false))
            adapter.notifyItemInserted(messages.size - 1)

            voiceOrb.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
            statusText.text = "Maya is speaking..."

            elevenLabsService.speak(response)
            statusText.text = "Ready"
        }
    }

    private fun checkAudioPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            return false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizerManager?.release()
        elevenLabsService.release()
    }
}
