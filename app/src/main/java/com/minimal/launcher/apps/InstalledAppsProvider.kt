package com.minimal.launcher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore

data class AppEntry(val label: String, val packageName: String)

class InstalledAppsProvider(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    @Suppress("DEPRECATION")
    fun launchableApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** First-run defaults: phone, messages, browser, camera (resolved by intent, not package name). */
    fun defaultPackages(): List<String> = listOfNotNull(
        pkgFor(Intent(Intent.ACTION_DIAL)),
        pkgFor(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)),
        pkgFor(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)),
        pkgFor(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
    ).distinct()

    private fun pkgFor(intent: Intent): String? =
        pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?.takeIf { it != context.packageName && pm.getLaunchIntentForPackage(it) != null }
}
