package com.example.util

import android.os.Build

object DeviceUtils {
    /**
     * Determines whether the app is currently running inside an Android emulator
     * or virtual container environment (such as Google Cloud Cuttlefish, QEMU, Ranchu,
     * x86/x86_64 virtual container devices, or AI Studio preview).
     */
    fun isEmulator(): Boolean {
        // 1. All debug and development builds (AI Studio preview, local testing, Robolectric)
        // must use safe simulated behavior to avoid Binder IPC buffer contention (-ENOSPC)
        // and remote Play Services / AdMob crashes.
        if (com.example.BuildConfig.DEBUG) {
            return true
        }

        // 2. Architecture check: AI Studio and containerized Android emulators execute on x86 or x86_64.
        // Modern physical Android devices run ARM (arm64-v8a / armeabi-v7a).
        val hasX86 = Build.SUPPORTED_ABIS.any { it.contains("x86", ignoreCase = true) }
        if (hasX86) {
            return true
        }

        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val tags = (Build.TAGS ?: "").lowercase()
        val bootloader = (Build.BOOTLOADER ?: "").lowercase()

        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || fingerprint.contains("cf_")
                || fingerprint.contains("vsoc")
                || fingerprint.contains("cutf")
                || fingerprint.contains("test-keys")
                || tags.contains("test-keys")
                || bootloader.contains("unknown")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for")
                || model.contains("cuttlefish")
                || model.contains("cf ")
                || model.contains("cf_")
                || manufacturer.contains("genymotion")
                || product.contains("sdk")
                || product.contains("google_sdk")
                || product.contains("emulator")
                || product.contains("simulator")
                || product.contains("vbox")
                || product.contains("cf_")
                || product.contains("cvd")
                || product.contains("cuttlefish")
                || product.contains("aosp_cf")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("cutf")
                || hardware.contains("vsoc")
                || hardware.contains("qemu")
                || device.contains("generic")
                || device.contains("vsoc")
                || device.contains("cutf")
                || device.contains("emulator")
                || board.contains("cutf")
                || board.contains("vsoc")
                || (brand.startsWith("generic") && device.startsWith("generic"))
    }
}
