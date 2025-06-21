package com.example.watertracker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar // Add this import
import java.util.concurrent.TimeUnit // Add this import
import androidx.work.OneTimeWorkRequestBuilder // Add this import
import androidx.work.WorkManager // Add this import

class ResetCounterWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    private val prefsName = "WaterTrackerPrefs"
    private val countKey = "waterCount"
    private val dateKey = "lastUpdateDate"
    private val TAG = "ResetCounterWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started for daily reset.")

        // --- 1. Reset the counter in SharedPreferences ---
        val prefs = applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(countKey, 0) // Reset count to 0
            .putString(dateKey, getCurrentDateString()) // Store today's date
            .apply()

        // --- 2. Force the widget to update its UI ---
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val componentName = ComponentName(applicationContext, WaterWidgetProvider::class.java)
        val allAppWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        for (appWidgetId in allAppWidgetIds) {
            val views = RemoteViews(applicationContext.packageName, R.layout.water_widget_layout)
            // Update the UI elements to show the reset state
            views.setTextViewText(R.id.widget_tv_counter, "0")
            views.setInt(R.id.widget_background_image, "setImageLevel", 0)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        Log.d(TAG, "Worker finished. Counter reset and widgets updated.")

        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialDelay = nextMidnight.timeInMillis - now.timeInMillis
        val dailyResetRequest = OneTimeWorkRequestBuilder<ResetCounterWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "daily_reset_work",
            androidx.work.ExistingWorkPolicy.REPLACE,
            dailyResetRequest
        )
        Log.d(TAG, "Re-scheduled next reset in $initialDelay ms")

        return Result.success()
    }

    private fun getCurrentDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }
}