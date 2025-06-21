package com.example.watertracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.content.ComponentName
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import java.util.Calendar

class WaterWidgetProvider : AppWidgetProvider() {

    private val prefsName = "WaterTrackerPrefs"
    private val countKey = "waterCount"
    private val dateKey = "lastUpdateDate"
    private val ACTION_ADD_WATER = "com.example.watertracker.ACTION_ADD_WATER"
    private val TAG = "WaterWidgetProvider"
    private val cups = 10

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "First widget enabled. Scheduling a precise daily reset.")

        // --- NEW LOGIC TO SCHEDULE A ONE-TIME WORKER FOR NEXT MIDNIGHT ---

        // 1. Get the current time
        val now = Calendar.getInstance()

        // 2. Get the time for the next midnight
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1) // Move to tomorrow
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 3. Calculate the time difference (initial delay)
        val initialDelay = nextMidnight.timeInMillis - now.timeInMillis
        Log.d(TAG, "Next reset is in $initialDelay ms")

        // 4. Create a OneTimeWorkRequest with that calculated delay
        val dailyResetRequest = OneTimeWorkRequestBuilder<ResetCounterWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // 5. Enqueue the work with a unique name, replacing any existing work
        WorkManager.getInstance(context).enqueueUniqueWork(
            "daily_reset_work",
            androidx.work.ExistingWorkPolicy.REPLACE, // Replace the old work with the new one
            dailyResetRequest
        )
    }

    override fun onDisabled(context: Context) {1
        super.onDisabled(context)
        // This is called when the last widget is removed
        Log.d(TAG, "Last widget disabled. Cancelling daily reset worker.")
        WorkManager.getInstance(context).cancelUniqueWork("daily_reset_work")
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (ACTION_ADD_WATER == intent.action) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

            // Get the last saved date and count
            val savedDate = prefs.getString(dateKey, "")
            var currentCount = prefs.getInt(countKey, 0)

            // Get today's date as a simple string (e.g., "2025-06-21")
            val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            // Check if it's a new day
            if (savedDate != todayDate) {
                // It's a new day, so reset the counter to 1 (for the current press)
                currentCount = 1
                Log.d(TAG, "New day detected. Resetting count to 1.")
            } else {
                // It's the same day, so increment normally
                currentCount++
                if (currentCount > cups) {
                    currentCount = 0 // Reset if it goes over 8
                }
            }

            // Save the new count AND the new date for today
            prefs.edit()
                .putInt(countKey, currentCount)
                .putString(dateKey, todayDate)
                .apply()

            Log.d(TAG, "New count is: $currentCount for date: $todayDate")

            // Manually trigger an update for all widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WaterWidgetProvider::class.java)
            val allAppWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, allAppWidgetIds)
        }
    }


    // Replace your entire updateAppWidget function with this new version
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val count = prefs.getInt(countKey, 0)

        val views = RemoteViews(context.packageName, R.layout.water_widget_layout)

        // This logic remains the same
        val level = (count * 10000) / cups
        views.setInt(R.id.widget_background_image, "setImageLevel", level)
        views.setTextViewText(R.id.widget_tv_counter, count.toString())

        // Create the PendingIntent (this logic is the same as before)
        val intent = Intent(context, WaterWidgetProvider::class.java)
        intent.action = ACTION_ADD_WATER

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // --- THIS IS THE KEY CHANGE ---
        // Attach the click listener to the entire widget's root layout
        views.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent)


        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}