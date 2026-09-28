package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceNotificationManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    var isVoiceEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stop()
            }
        }
    var speakSiteChanges: Boolean = true
    var speakSectorChanges: Boolean = true
    var speakBandInfo: Boolean = true
    var speakDistance: Boolean = true
    var speechRate: Float = 1.0f

    private var lastSiteId: String? = null
    private var lastSectorId: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceNotification", "Language not supported")
            } else {
                isInitialized = true
                tts?.setSpeechRate(speechRate)
            }
        } else {
            Log.e("VoiceNotification", "TTS Initialization failed")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun speak(text: String, overrideEnabled: Boolean = false) {
        if (!isVoiceEnabled) return
        if (!isInitialized) return

        tts?.setSpeechRate(speechRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TELECOM_TTS_ID_${System.currentTimeMillis()}")
    }

    /**
     * Checks if site or sector changed and triggers speech alert.
     */
    fun checkAndAnnounceCellChange(
        siteId: String,
        sectorId: String,
        bandName: String,
        siteName: String? = null,
        distanceMeters: Double? = null
    ) {
        if (!isVoiceEnabled) return
        if (siteId == "N/A" || siteId.isBlank()) return

        val siteChanged = lastSiteId != null && lastSiteId != siteId
        val sectorChanged = lastSectorId != null && lastSectorId != sectorId && !siteChanged
        val isFirstTime = lastSiteId == null

        if (siteChanged || sectorChanged || isFirstTime) {
            announceCurrentTelemetry(siteId, sectorId, bandName, siteName, distanceMeters)
        }

        lastSiteId = siteId
        lastSectorId = sectorId
    }

    /**
     * Force re-announces the active site ID, sector ID, frequency band, and distance via voice.
     */
    fun announceCurrentTelemetry(
        siteId: String,
        sectorId: String,
        bandName: String,
        siteName: String? = null,
        distanceMeters: Double? = null
    ) {
        if (!isVoiceEnabled) return
        if (siteId == "N/A" || siteId.isBlank()) {
            speak("No active cell site connected.")
            return
        }

        val displayName = if (!siteName.isNullOrBlank()) siteName else "Site $siteId"
        val sb = StringBuilder()
        sb.append("Connected to $displayName. ")
        if (sectorId != "N/A" && sectorId.isNotBlank()) {
            sb.append("Sector $sectorId. ")
        }
        if (bandName.isNotBlank() && bandName != "Unknown") {
            sb.append("Frequency band $bandName. ")
        }
        if (distanceMeters != null && distanceMeters > 0) {
            val distText = if (distanceMeters >= 1000) {
                String.format("%.1f kilometers", distanceMeters / 1000.0)
            } else {
                "${distanceMeters.toInt()} meters"
            }
            sb.append("Distance $distText.")
        }

        val text = sb.toString().trim()
        if (text.isNotBlank()) {
            speak(text)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
