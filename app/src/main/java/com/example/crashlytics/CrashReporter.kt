package com.example.crashlytics

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Reusable helper for Firebase Crashlytics logging, non-fatal exceptions, and user keys.
 */
object CrashReporter {
    private const val TAG = "CrashReporter"

    private val crashlytics: FirebaseCrashlytics?
        get() = try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseCrashlytics unavailable: ${e.message}")
            null
        }

    /**
     * Records a non-fatal exception to Firebase Crashlytics dashboard.
     */
    fun recordException(throwable: Throwable) {
        Log.e(TAG, "Recording non-fatal exception to Crashlytics: ${throwable.message}", throwable)
        crashlytics?.recordException(throwable)
    }

    /**
     * Adds breadcrumb logging into Crashlytics reports.
     */
    fun log(message: String) {
        Log.d(TAG, "[Crashlytics] $message")
        crashlytics?.log(message)
    }

    fun setCustomKey(key: String, value: String) {
        crashlytics?.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        crashlytics?.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Int) {
        crashlytics?.setCustomKey(key, value)
    }

    fun setUserId(userId: String) {
        crashlytics?.setUserId(userId)
    }
}
