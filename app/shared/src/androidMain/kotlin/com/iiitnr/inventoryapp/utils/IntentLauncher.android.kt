package com.iiitnr.inventoryapp.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import java.lang.ref.WeakReference

private class AndroidIntentLauncher {
    companion object {
        var activityRef: WeakReference<Activity>? = null
    }

    fun launch(
        uri: String,
        chooserTitle: String,
    ): LaunchResult {
        val activity = activityRef?.get() ?: return LaunchResult.NoAppAvailable
        val viewIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            activity.startActivity(Intent.createChooser(viewIntent, chooserTitle))
            LaunchResult.Launched
        } catch (e: ActivityNotFoundException) {
            LaunchResult.Error(e.message ?: "Could not open the link")
        } catch (e: SecurityException) {
            LaunchResult.Error(e.message ?: "Could not open the link")
        }
    }
}

fun setIntentLauncherActivity(activity: Activity) {
    AndroidIntentLauncher.activityRef = WeakReference(activity)
}

actual fun createIntentLauncher(): IntentLauncher =
    object : IntentLauncher {
        override fun launchUpi(uri: String): LaunchResult = AndroidIntentLauncher().launch(uri, "Pay with UPI")

        override fun launchUrl(url: String): LaunchResult = AndroidIntentLauncher().launch(url, "Open link")
    }
