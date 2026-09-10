package org.schabi.newpipe

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.IOException
import org.schabi.newpipe.updates.CompatibilityPolicyStore
import org.schabi.newpipe.updates.OwnedUpdateClient
import org.schabi.newpipe.util.ReleaseVersionUtil

class NewVersionWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    /**
     * Method to compare the current and latest available app version.
     * If a newer version is available, we show the update notification.
     *
     * @param versionName    Name of new version
     * @param apkLocationUrl Url with the new apk
     * @param versionCode    Code of new version
     */
    private fun compareAppVersionAndShowNotification(
        versionName: String,
        apkLocationUrl: String?,
        versionCode: Int
    ) {
        if (BuildConfig.VERSION_CODE >= versionCode) {
            if (inputData.getBoolean(IS_MANUAL, false)) {
                // Show toast stating that the app is up-to-date if the update check was manual.
                ContextCompat.getMainExecutor(applicationContext).execute {
                    Toast.makeText(
                        applicationContext,
                        R.string.app_update_unavailable_toast,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            return
        }

        // A pending intent to open the apk location url in the browser.
        val intent = Intent(Intent.ACTION_VIEW, apkLocationUrl?.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntentCompat.getActivity(
            applicationContext,
            0,
            intent,
            0,
            false
        )
        val channelId = applicationContext.getString(R.string.app_update_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_newpipe_update)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setContentTitle(
                applicationContext.getString(R.string.app_update_available_notification_title)
            )
            .setContentText(
                applicationContext.getString(
                    R.string.app_update_available_notification_text,
                    versionName
                )
            )

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(2000, notificationBuilder.build())
        }
    }

    private fun checkNewVersion() {
        if (!ReleaseVersionUtil.isReleaseApk) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val manual = inputData.getBoolean(IS_MANUAL, false)
        if (!manual) {
            if (!prefs.getBoolean(applicationContext.getString(R.string.update_app_key), false)) return
            val expiry = prefs.getLong(applicationContext.getString(R.string.update_expiry_key), 0)
            if (!ReleaseVersionUtil.isLastUpdateCheckExpired(expiry)) return
        }
        val client = OwnedUpdateClient(applicationContext)
        val update = client.latest(prefs.getString(CHANNEL_KEY, "stable") == "beta")
        if (update != null) {
            compareAppVersionAndShowNotification(update.versionName, update.apkUrl, update.versionCode)
        } else if (manual) {
            showToast(R.string.newpipet_no_release)
        }
        // Cache only a successful response. A failed check must remain retryable.
        prefs.edit {
            putLong(
                applicationContext.getString(R.string.update_expiry_key),
                java.time.Instant.now().epochSecond + 6 * 3600
            )
            putString(STATUS_KEY, "ok")
        }
        if (prefs.getBoolean(CompatibilityPolicyStore.ENABLED, false)) {
            // Keep a working local policy if this optional endpoint is unavailable or invalid.
            runCatching {
                client.compatibilityDocument()?.let { CompatibilityPolicyStore.install(applicationContext, it) }
            }
        }
    }

    private fun showToast(message: Int) {
        ContextCompat.getMainExecutor(applicationContext).execute {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun doWork(): Result = try {
        checkNewVersion()
        Result.success()
    } catch (exception: Exception) {
        Log.w(TAG, "Update check failed: ${exception.javaClass.simpleName}")
        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit {
            putString(STATUS_KEY, "failed")
        }
        if (inputData.getBoolean(IS_MANUAL, false)) showToast(R.string.newpipet_update_failed)
        if (exception is IOException && runAttemptCount < 2) Result.retry() else Result.failure()
    }

    companion object {
        private val DEBUG = MainActivity.DEBUG
        private val TAG = NewVersionWorker::class.java.simpleName
        private const val IS_MANUAL = "isManual"
        const val CHANNEL_KEY = "newpipet_update_channel"
        const val STATUS_KEY = "newpipet_update_status"

        @JvmStatic
        fun enqueueNewVersionCheckingWork(context: Context, isManual: Boolean) {
            val channel = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(CHANNEL_KEY, "stable")
            val request = OneTimeWorkRequestBuilder<NewVersionWorker>()
                .setInputData(workDataOf(IS_MANUAL to isManual))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "newpipet-updates-$channel-$isManual", ExistingWorkPolicy.KEEP, request
            )
        }
    }
}
