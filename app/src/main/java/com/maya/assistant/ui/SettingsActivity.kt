package com.maya.assistant.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.maya.assistant.MayaApp
import com.maya.assistant.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val editGemini: EditText = findViewById(R.id.editGeminiKey)
        val editElevenLabs: EditText = findViewById(R.id.editElevenLabsKey)
        val editVoiceId: EditText = findViewById(R.id.editVoiceId)
        val editPrompt: EditText = findViewById(R.id.editSystemPrompt)
        val btnSave: Button = findViewById(R.id.btnSaveSettings)

        val prefs = MayaApp.instance.prefs

        editGemini.setText(prefs.geminiApiKey)
        editElevenLabs.setText(prefs.elevenLabsApiKey)
        editVoiceId.setText(prefs.elevenLabsVoiceId)
        editPrompt.setText(prefs.systemPrompt)

        btnSave.setOnClickListener {
            prefs.geminiApiKey = editGemini.text.toString().trim()
            prefs.elevenLabsApiKey = editElevenLabs.text.toString().trim()
            prefs.elevenLabsVoiceId = editVoiceId.text.toString().trim()
            prefs.systemPrompt = editPrompt.text.toString().trim()

            Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
