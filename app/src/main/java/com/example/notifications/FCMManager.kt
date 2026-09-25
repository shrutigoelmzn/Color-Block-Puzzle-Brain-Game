package com.example.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object FCMManager {
    private const val TAG = "FCMManager"

    /**
     * Initializes notification channels and retrieves the device's FCM token.
     */
    fun initialize(context: Context) {
        NotificationHelper.createNotificationChannel(context)

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.d(TAG, "FCM registration token not available or offline: ${task.exception?.message}")
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "FCM Token ready: $token")
            }
        } catch (e: Throwable) {
            Log.d(TAG, "FCM token fetch deferred: ${e.message}")
        }
    }
}
