package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import android.os.Build
import com.example.MainActivity
import com.example.R
import com.example.utils.TelephonyHelper

class NetworkModeWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_MODE_AUTO = "com.example.widget.ACTION_MODE_AUTO"
        const val ACTION_WIDGET_MODE_4G = "com.example.widget.ACTION_MODE_4G"
        const val ACTION_WIDGET_MODE_5G = "com.example.widget.ACTION_MODE_5G"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        val action = intent.action
        if (action == ACTION_WIDGET_MODE_AUTO || action == ACTION_WIDGET_MODE_4G || action == ACTION_WIDGET_MODE_5G) {
            
            // Check if PRO version is unlocked (as widget is limited to PRO version)
            val prefs = context.getSharedPreferences("smart_optimizer_prefs", Context.MODE_PRIVATE)
            val isProUnlocked = prefs.getBoolean("pro_unlocked", false)
            
            if (!isProUnlocked) {
                Toast.makeText(context, "Network Widget requires Pro Upgrade!", Toast.LENGTH_LONG).show()
                // Launch MainActivity to purchase
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
                return
            }

            val mode = when (action) {
                ACTION_WIDGET_MODE_5G -> TelephonyHelper.MODE_5G_ONLY
                ACTION_WIDGET_MODE_4G -> TelephonyHelper.MODE_4G_ONLY
                else -> TelephonyHelper.MODE_AUTO
            }

            // Apply Network Switch
            val success = TelephonyHelper.setNetworkMode(context, -1, mode)
            if (success) {
                Toast.makeText(context, "Mode switched to $mode", Toast.LENGTH_SHORT).show()
                
                // Save state
                prefs.edit().putString("active_manual_mode", mode).apply()
            } else {
                Toast.makeText(context, "API access blocked. Use hidden settings fallback.", Toast.LENGTH_SHORT).show()
                TelephonyHelper.launchHiddenRadioInfo(context)
            }

            // Force visual widget update across all instances
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, NetworkModeWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Find currently applied mode or preference
        val prefs = context.getSharedPreferences("smart_optimizer_prefs", Context.MODE_PRIVATE)
        val currentMode = prefs.getString("active_manual_mode", TelephonyHelper.MODE_AUTO) ?: TelephonyHelper.MODE_AUTO
        views.setTextViewText(R.id.widget_status, "Current: $currentMode")

        // Hook up pending intents for the buttons
        views.setOnClickPendingIntent(R.id.btn_widget_auto, getPendingSelfIntent(context, ACTION_WIDGET_MODE_AUTO))
        views.setOnClickPendingIntent(R.id.btn_widget_4g, getPendingSelfIntent(context, ACTION_WIDGET_MODE_4G))
        views.setOnClickPendingIntent(R.id.btn_widget_5g, getPendingSelfIntent(context, ACTION_WIDGET_MODE_5G))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, javaClass).apply { this.action = action }
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flag)
    }
}
