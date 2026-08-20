package com.jimz011apps.hki7.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Android 17 is the first release that enforces local-network access for targetSdk 37 apps. */
const val ANDROID_17_API_LEVEL = 37
const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

internal fun localNetworkPermissionRequired(sdkInt: Int, permissionGranted: Boolean): Boolean =
    sdkInt >= ANDROID_17_API_LEVEL && !permissionGranted

/** True on older Android releases, or once Android 17's Nearby devices permission is granted. */
fun canAccessLocalNetwork(context: Context): Boolean {
    val granted = ContextCompat.checkSelfPermission(
        context,
        LOCAL_NETWORK_PERMISSION,
    ) == PackageManager.PERMISSION_GRANTED
    return !localNetworkPermissionRequired(Build.VERSION.SDK_INT, granted)
}
