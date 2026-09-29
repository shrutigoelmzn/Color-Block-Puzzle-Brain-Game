package com.example.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.example.BuildConfig
import com.example.util.DeviceUtils
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class UpdateUIState {
    object Idle : UpdateUIState()
    object Checking : UpdateUIState()
    data class UpdateAvailable(
        val appUpdateInfo: AppUpdateInfo,
        val isFlexibleAllowed: Boolean,
        val isImmediateAllowed: Boolean,
        val stalenessDays: Int?
    ) : UpdateUIState()
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long,
        val progressPercentage: Int
    ) : UpdateUIState()
    object Downloaded : UpdateUIState()
    object UpToDate : UpdateUIState()
    data class Error(val message: String) : UpdateUIState()
}

/**
 * Handles Google Play In-App Updates (Flexible and Immediate flows).
 *
 * Supports:
 * - Automatic background checks on app start.
 * - On-demand manual update checks from Settings.
 * - Flexible background downloading with in-app completion prompt.
 * - Safe graceful fallback on emulators / devices without Google Play Services.
 */
object InAppUpdateManager {
    private const val TAG = "InAppUpdateManager"
    const val PLAY_STORE_PACKAGE = "com.sharma.color_block_building"

    private var appUpdateManager: AppUpdateManager? = null
    private var isListenerRegistered = false

    private val _updateState = MutableStateFlow<UpdateUIState>(UpdateUIState.Idle)
    val updateState: StateFlow<UpdateUIState> = _updateState.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state: InstallState ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                val progress = if (totalBytes > 0) {
                    ((bytesDownloaded * 100) / totalBytes).toInt()
                } else 0
                Log.d(TAG, "Update downloading: $progress% ($bytesDownloaded/$totalBytes)")
                _updateState.value = UpdateUIState.Downloading(bytesDownloaded, totalBytes, progress)
            }
            InstallStatus.DOWNLOADED -> {
                Log.i(TAG, "In-app update downloaded and ready to install")
                _updateState.value = UpdateUIState.Downloaded
            }
            InstallStatus.FAILED -> {
                Log.w(TAG, "Update download failed with error code: ${state.installErrorCode()}")
                _updateState.value = UpdateUIState.Error("Update download failed (code ${state.installErrorCode()})")
            }
            InstallStatus.CANCELED -> {
                Log.d(TAG, "Update download was canceled")
                _updateState.value = UpdateUIState.Idle
            }
            InstallStatus.INSTALLING -> {
                Log.d(TAG, "Update is currently installing")
            }
            else -> {}
        }
    }

    /**
     * Initializes the AppUpdateManager and registers the flexible download listener.
     */
    fun initialize(context: Context) {
        if (DeviceUtils.isEmulator()) {
            Log.d(TAG, "Running in emulator; in-app update checks bypassed")
            return
        }

        try {
            if (appUpdateManager == null) {
                appUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)
            }
            registerListener()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize AppUpdateManager: ${e.message}")
        }
    }

    private fun registerListener() {
        if (!isListenerRegistered && appUpdateManager != null) {
            try {
                appUpdateManager?.registerListener(installStateUpdatedListener)
                isListenerRegistered = true
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to register InstallStateUpdatedListener: ${e.message}")
            }
        }
    }

    fun unregisterListener() {
        if (isListenerRegistered && appUpdateManager != null) {
            try {
                appUpdateManager?.unregisterListener(installStateUpdatedListener)
                isListenerRegistered = false
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to unregister InstallStateUpdatedListener: ${e.message}")
            }
        }
    }

    /**
     * Checks if an update is available on Google Play.
     */
    fun checkForUpdate(
        context: Context,
        isManualCheck: Boolean = false,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        if (DeviceUtils.isEmulator()) {
            Log.d(TAG, "Running in emulator; simulated up-to-date state")
            _updateState.value = UpdateUIState.UpToDate
            onResult?.invoke(false)
            return
        }

        if (appUpdateManager == null) {
            try {
                appUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)
                registerListener()
            } catch (e: Throwable) {
                Log.w(TAG, "AppUpdateManager initialization error: ${e.message}")
                _updateState.value = UpdateUIState.Error("Update service unavailable")
                onResult?.invoke(false)
                return
            }
        }

        _updateState.value = UpdateUIState.Checking

        try {
            appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
                val availability = appUpdateInfo.updateAvailability()
                Log.d(TAG, "App update availability: $availability")

                when (availability) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                        val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        val stalenessDays = appUpdateInfo.clientVersionStalenessDays()

                        _updateState.value = UpdateUIState.UpdateAvailable(
                            appUpdateInfo = appUpdateInfo,
                            isFlexibleAllowed = isFlexibleAllowed,
                            isImmediateAllowed = isImmediateAllowed,
                            stalenessDays = stalenessDays
                        )
                        onResult?.invoke(true)
                    }
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        // Update already in progress
                        Log.i(TAG, "Update in progress")
                        _updateState.value = UpdateUIState.Downloading(0, 0, 0)
                        onResult?.invoke(true)
                    }
                    else -> {
                        Log.d(TAG, "App is up to date (versionCode: ${BuildConfig.VERSION_CODE})")
                        _updateState.value = UpdateUIState.UpToDate
                        onResult?.invoke(false)
                    }
                }
            }?.addOnFailureListener { e ->
                Log.d(TAG, "Check for update failed: ${e.message}")
                if (isManualCheck) {
                    _updateState.value = UpdateUIState.Error("Could not connect to Google Play Store.")
                } else {
                    _updateState.value = UpdateUIState.Idle
                }
                onResult?.invoke(false)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "checkForUpdate exception: ${e.message}")
            _updateState.value = if (isManualCheck) UpdateUIState.Error(e.message ?: "Update check failed") else UpdateUIState.Idle
            onResult?.invoke(false)
        }
    }

    /**
     * Starts the flexible or immediate update flow.
     */
    fun startUpdate(
        activity: Activity,
        appUpdateInfo: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        updateType: Int = AppUpdateType.FLEXIBLE
    ): Boolean {
        return try {
            val options = AppUpdateOptions.newBuilder(updateType).build()
            appUpdateManager?.startUpdateFlowForResult(appUpdateInfo, launcher, options)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "startUpdate failed: ${e.message}")
            _updateState.value = UpdateUIState.Error("Failed to start update flow: ${e.message}")
            false
        }
    }

    /**
     * Completes flexible update by restarting the application.
     */
    fun completeUpdate() {
        try {
            appUpdateManager?.completeUpdate()
        } catch (e: Throwable) {
            Log.e(TAG, "completeUpdate failed: ${e.message}")
        }
    }

    /**
     * Called in Activity onResume to handle pending downloaded updates or resume immediate flows.
     */
    fun onResume(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (DeviceUtils.isEmulator()) return

        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                _updateState.value = UpdateUIState.Downloaded
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Resume immediate update flow
                try {
                    val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    appUpdateManager?.startUpdateFlowForResult(appUpdateInfo, launcher, options)
                } catch (e: Throwable) {
                    Log.d(TAG, "Failed to resume immediate update: ${e.message}")
                }
            }
        }
    }

    /**
     * Handles activity result callback from update flow.
     */
    fun handleActivityResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "Update flow canceled or failed with resultCode: $resultCode")
            if (_updateState.value !is UpdateUIState.Downloaded) {
                _updateState.value = UpdateUIState.Idle
            }
        }
    }

    /**
     * Fallback to direct Play Store page when user taps "Open in Play Store".
     */
    fun openPlayStore(context: Context) {
        val packageName = context.packageName ?: PLAY_STORE_PACKAGE
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun dismissState() {
        _updateState.value = UpdateUIState.Idle
    }
}
