package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.LocationProfile
import com.example.data.LocationProfileRepository
import com.example.utils.TelephonyHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SmartOptimizerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var database: AppDatabase
    private lateinit var repository: LocationProfileRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val handler = Handler(Looper.getMainLooper())
    private var optimizerRunnable: Runnable? = null

    private var lastModeApplied: String = TelephonyHelper.MODE_AUTO
    private val CHANNEL_ID = "smart_optimizer_channel_id"
    private val NOTIFICATION_ID = 5123

    // Real-time cached parameters to show in UI
    companion object {
        const val TAG = "SmartOptimizerService"
        var isServiceRunning = false
        var currentStatusText = "Active: Monitoring network status..."
        var lastRunTimestamp = 0L

        // Service actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TRIGGER_CHECK = "ACTION_TRIGGER_CHECK"
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                scope.launch {
                    runOptimizationCheck()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        database = AppDatabase.getDatabase(this)
        repository = LocationProfileRepository(database.locationProfileDao())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Run checker every 60 seconds to save battery, but keep monitoring real-time
        setupPeriodicOptimization()
        Log.d(TAG, "Smart Optimizer Service Created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand action: $action")

        if (action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground Service
        startForeground(NOTIFICATION_ID, buildStatusNotification("Starting Smart Optimizer..."))

        scope.launch {
            runOptimizationCheck()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupPeriodicOptimization() {
        optimizerRunnable = object : Runnable {
            override fun run() {
                scope.launch {
                    runOptimizationCheck()
                }
                // Run optimization check every 2 minutes for light, battery-saving check
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(2))
            }
        }
        handler.post(optimizerRunnable!!)
    }

    @SuppressLint("MissingPermission")
    private suspend fun runOptimizationCheck() {
        Log.d(TAG, "Executing Optimization Scan...")
        lastRunTimestamp = System.currentTimeMillis()

        // Read SharedPreferences toggles
        val prefs = getSharedPreferences("smart_optimizer_prefs", Context.MODE_PRIVATE)
        val isBatteryEnabled = prefs.getBoolean("battery_optimization_enabled", true)
        val isSignalEnabled = prefs.getBoolean("signal_optimization_enabled", true)
        val isLocationEnabled = prefs.getBoolean("location_optimization_enabled", true)
        val isCarModeEnabled = prefs.getBoolean("car_mode_enabled", false)

        // Safety fallback: If general safety mode disabled, return
        val isSafetyModeActive = prefs.getBoolean("safety_mode_enabled", true)
        val isProUnlocked = prefs.getBoolean("pro_unlocked", false)

        // Force fallback if user is in free tier and somehow service is still running
        if (!isProUnlocked) {
            currentStatusText = "Optimizer requires Pro Version Upgrade"
            updateNotification("Optimization inactive. Please unlock PRO.")
            return
        }

        var targetMode = TelephonyHelper.MODE_AUTO
        var reason = "Default mode config."

        // 1. Check Battery status
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()) else 100f

        if (isBatteryEnabled && batteryPct < 15f) {
            targetMode = TelephonyHelper.MODE_4G_ONLY
            reason = "Battery low (${batteryPct.toInt()}% < 15%). Switched to 4G saver."
        } else {
            // 2. Check Car Mode (Simulated speed / sensor context in background)
            if (isCarModeEnabled) {
                // If driving, we require a highly stable, seamless 4G signal (no frequent 5G/NR handover drops)
                targetMode = TelephonyHelper.MODE_4G_ONLY
                reason = "Car Mode active. Applying stable LTE band."
            } else {
                // 3. Check Signal Strength (if in 5G Mode, and strength falls below -110dBm, switch down)
                val signalDbm = TelephonyHelper.getSignalStrengthDbm(this)
                if (isSignalEnabled && signalDbm < -110) {
                    targetMode = TelephonyHelper.MODE_4G_ONLY
                    reason = "Unstable 5G Signal ($signalDbm dBm < -110 dBm). Forcing LTE."
                } else {
                    // 4. Location-based profile checks
                    if (isLocationEnabled) {
                        try {
                            val activeProfiles = repository.activeProfiles.first()
                            var matchedProfile: LocationProfile? = null

                            // Synchronous location fetching to avoid suspending indefinitely in background
                            val locationTask = fusedLocationClient.lastLocation
                            val location: Location? = Tasks.await(locationTask, 4, TimeUnit.SECONDS)

                            if (location != null) {
                                for (profile in activeProfiles) {
                                    val profileLoc = Location("profile").apply {
                                        latitude = profile.latitude
                                        longitude = profile.longitude
                                    }
                                    if (location.distanceTo(profileLoc) <= profile.radiusInMeters) {
                                        matchedProfile = profile
                                        break
                                    }
                                }
                            }

                            if (matchedProfile != null) {
                                targetMode = matchedProfile.preferredMode
                                reason = "Arrived at location profile '${matchedProfile.name}'. Applying ${matchedProfile.preferredMode}."
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Location profile check failed: ${e.message}")
                        }
                    }
                }
            }
        }

        // Apply mode switch if it differs from current applied mode
        if (targetMode != lastModeApplied) {
            // Try to set mode
            val success = TelephonyHelper.setNetworkMode(this, -1, targetMode)
            if (success) {
                lastModeApplied = targetMode
                currentStatusText = "Active: $reason"
                updateNotification("Status: $reason")
            } else {
                if (isSafetyModeActive) {
                    // Fallback to auto
                    TelephonyHelper.setNetworkMode(this, -1, TelephonyHelper.MODE_AUTO)
                    lastModeApplied = TelephonyHelper.MODE_AUTO
                    currentStatusText = "Fallback (Safety): Auto mode applied. Device profile locked."
                    updateNotification("Safety Fallback: Auto mode active.")
                } else {
                    currentStatusText = "Failure: Cannot write band type. Launching settings fallbacks."
                }
            }
        } else {
            currentStatusText = "Optimized: $reason (No change needed)"
            val signal = TelephonyHelper.getSignalStrengthDbm(this)
            updateNotification("Active: $lastModeApplied ($signal dBm)")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Smart Network Optimizer Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildStatusNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("5G Smart Network Optimizer")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call) // Built-in phone icon placeholder, robust and always exists
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildStatusNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        isServiceRunning = false
        optimizerRunnable?.let { handler.removeCallbacks(it) }
        unregisterReceiver(batteryReceiver)
        job.cancel()
        super.onDestroy()
        Log.d(TAG, "Smart Optimizer Service Destroyed.")
    }
}
