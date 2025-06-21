package com.example.watertracker

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvWaterCount: TextView
    private val prefsName = "WaterTrackerPrefs"
    private val countKey = "waterCount"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvWaterCount = findViewById(R.id.tvWaterCount)
        val btnAddWater: Button = findViewById(R.id.btnAddWater)

        updateCountDisplay()

        btnAddWater.setOnClickListener {
            incrementWaterCount()
            updateCountDisplay()
        }
    }

    override fun onResume() {
        super.onResume()
        updateCountDisplay()
    }

    private fun incrementWaterCount() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        var currentCount = prefs.getInt(countKey, 0)
        currentCount++
        prefs.edit().putInt(countKey, currentCount).apply()
    }

    private fun updateCountDisplay() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(countKey, 0)
        tvWaterCount.text = currentCount.toString()
    }
}