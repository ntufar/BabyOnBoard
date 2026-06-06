package com.example.babyonboard.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri

class RaiseSosUseCase(private val context: Context) {
    fun execute(isCrashDetected: Boolean, confidence: Float) {
        if (isCrashDetected && confidence > 0.5f) {
            // Trigger SOS countdown (this would be handled by UI/Activity)
            // For now, just a placeholder
        }
    }
    
    fun dialEmergencyNumber(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
