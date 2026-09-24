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
                    Log.w(TAG, "Fetching FCM registration token failed: ${task.exception?.message}")
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.i(TAG, "==================================================")
                Log.i(TAG, "FIREBASE CLOUD MESSAGING (FCM) TOKEN:")
                Log.i(TAG, token)
                Log.i(TAG, "Copy this token to send test messages in Firebase Console")
                Log.i(TAG, "==================================================")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error initializing FCM: ${e.message}")
        }
    }
}
