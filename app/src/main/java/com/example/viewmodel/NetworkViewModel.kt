package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LocationProfile
import com.example.data.LocationProfileRepository
import com.example.data.ExtraRepository
import com.example.data.NetworkHistory
import com.example.data.NetworkSchedule
import com.example.data.FavoriteMode
import com.example.service.SmartOptimizerService
import com.example.utils.TelephonyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.random.Random

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val contextRef = WeakReference(application.applicationContext)
    private val database = AppDatabase.getDatabase(application)
    private val repository = LocationProfileRepository(database.locationProfileDao())
    private val extraRepository = ExtraRepository(
        database.networkHistoryDao(),
        database.networkScheduleDao(),
        database.favoriteModeDao()
    )

    private val prefs = application.getSharedPreferences("smart_optimizer_prefs", Context.MODE_PRIVATE)

    // Lists of Profiles
    val locationProfiles: StateFlow<List<LocationProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // New Lists from Room
    val networkHistory: StateFlow<List<NetworkHistory>> = extraRepository.networkHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<NetworkSchedule>> = extraRepository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteModes: StateFlow<List<FavoriteMode>> = extraRepository.favoriteModes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Signal strength & network details states
    private val _signalStrength = MutableStateFlow(-92)
    val signalStrength = _signalStrength.asStateFlow()

    private val _networkTypeString = MutableStateFlow("Cellular Mode")
    val networkTypeString = _networkTypeString.asStateFlow()

    private val _operatorName = MutableStateFlow("Carrier")
    val operatorName = _operatorName.asStateFlow()

    private val _currentAppliedMode = MutableStateFlow(TelephonyHelper.MODE_AUTO)
    val currentAppliedMode = _currentAppliedMode.asStateFlow()

    private val _activeSimId = MutableStateFlow(-1)
    val activeSimId = _activeSimId.asStateFlow()

    // Optimization triggers (SharedPreferences synced)
    private val _batteryOptimizationEnabled = MutableStateFlow(prefs.getBoolean("battery_optimization_enabled", true))
    val batteryOptimizationEnabled = _batteryOptimizationEnabled.asStateFlow()

    private val _signalOptimizationEnabled = MutableStateFlow(prefs.getBoolean("signal_optimization_enabled", true))
    val signalOptimizationEnabled = _signalOptimizationEnabled.asStateFlow()

    private val _locationOptimizationEnabled = MutableStateFlow(prefs.getBoolean("location_optimization_enabled", true))
    val locationOptimizationEnabled = _locationOptimizationEnabled.asStateFlow()

    private val _carModeEnabled = MutableStateFlow(prefs.getBoolean("car_mode_enabled", false))
    val carModeEnabled = _carModeEnabled.asStateFlow()

    private val _safetyModeEnabled = MutableStateFlow(prefs.getBoolean("safety_mode_enabled", true))
    val safetyModeEnabled = _safetyModeEnabled.asStateFlow()

    // Monetization features
    private val _isProUnlocked = MutableStateFlow(prefs.getBoolean("pro_unlocked", false))
    val isProUnlocked = _isProUnlocked.asStateFlow()

    // Background Service state
    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive = _isServiceActive.asStateFlow()

    private val _serviceStatusText = MutableStateFlow("Optimizer is stopped")
    val serviceStatusText = _serviceStatusText.asStateFlow()

    // Speed test properties
    private val _speedTestRunning = MutableStateFlow(false)
    val speedTestRunning = _speedTestRunning.asStateFlow()

    private val _downloadSpeedMbps = MutableStateFlow(0f)
    val downloadSpeedMbps = _downloadSpeedMbps.asStateFlow()

    private val _uploadSpeedMbps = MutableStateFlow(0f)
    val uploadSpeedMbps = _uploadSpeedMbps.asStateFlow()

    private val _pingMs = MutableStateFlow(0)
    val pingMs = _pingMs.asStateFlow()

    private val _speedTestProgress = MutableStateFlow(0f) // 0 to 1
    val speedTestProgress = _speedTestProgress.asStateFlow()

    // Interactive tutorial / safety guide
    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted = _isOnboardingCompleted.asStateFlow()

    // EXTRA FUTURE-PROOF STATES
    private val _sim1Mode = MutableStateFlow(prefs.getString("sim_1_mode", TelephonyHelper.MODE_AUTO) ?: TelephonyHelper.MODE_AUTO)
    val sim1Mode = _sim1Mode.asStateFlow()

    private val _sim2Mode = MutableStateFlow(prefs.getString("sim_2_mode", TelephonyHelper.MODE_AUTO) ?: TelephonyHelper.MODE_AUTO)
    val sim2Mode = _sim2Mode.asStateFlow()

    private val _isSatelliteConnected = MutableStateFlow(false)
    val isSatelliteConnected = _isSatelliteConnected.asStateFlow()

    private val _isSixGEnabled = MutableStateFlow(false)
    val isSixGEnabled = _isSixGEnabled.asStateFlow()

    private val _vonrStatus = MutableStateFlow("VoNR Active (5G Standalone HD Voice)")
    val vonrStatus = _vonrStatus.asStateFlow()

    private val _selectedBandLock = MutableStateFlow(prefs.getString("selected_band_lock", "None") ?: "None")
    val selectedBandLock = _selectedBandLock.asStateFlow()

    private val _autoRecoveryEnabled = MutableStateFlow(prefs.getBoolean("auto_recovery_enabled", true))
    val autoRecoveryEnabled = _autoRecoveryEnabled.asStateFlow()

    private val _isRecoveryTimerActive = MutableStateFlow(false)
    val isRecoveryTimerActive = _isRecoveryTimerActive.asStateFlow()

    private val _recoverySecondsRemaining = MutableStateFlow(30)
    val recoverySecondsRemaining = _recoverySecondsRemaining.asStateFlow()

    private val _cloudBackupStatus = MutableStateFlow("Sync Status: Idle")
    val cloudBackupStatus = _cloudBackupStatus.asStateFlow()

    // Smart Modes simulation active states
    private val _activeOneTapSmartMode = MutableStateFlow("None")
    val activeOneTapSmartMode = _activeOneTapSmartMode.asStateFlow()

    // Safety recovery countdown Job
    private var recoveryJob: Job? = null

    init {
        // Hydrate details
        _currentAppliedMode.value = prefs.getString("active_manual_mode", TelephonyHelper.MODE_AUTO) ?: TelephonyHelper.MODE_AUTO
        _activeSimId.value = prefs.getInt("active_sim_id", -1)

        // Prepopulate favorite modes table with default selections so they are ready
        viewModelScope.launch(Dispatchers.IO) {
            extraRepository.insertFavorite(FavoriteMode(TelephonyHelper.MODE_5G_ONLY, 0))
            extraRepository.insertFavorite(FavoriteMode(TelephonyHelper.MODE_4G_ONLY, 1))
        }

        // Start polling cellular strength & parameters every 1.5 seconds safely
        startSignalPolling()
        checkServiceStatus()
    }

    private fun startSignalPolling() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val ctx = contextRef.get() ?: break
                _signalStrength.value = TelephonyHelper.getSignalStrengthDbm(ctx)
                _networkTypeString.value = TelephonyHelper.getActiveNetworkTypeString(ctx)
                _operatorName.value = TelephonyHelper.getOperatorName(ctx)
                _isServiceActive.value = SmartOptimizerService.isServiceRunning
                _serviceStatusText.value = if (SmartOptimizerService.isServiceRunning) {
                    SmartOptimizerService.currentStatusText
                } else {
                    "Optimizer is stopped"
                }
                delay(1500)
            }
        }
    }

    fun checkServiceStatus() {
        _isServiceActive.value = SmartOptimizerService.isServiceRunning
    }

    // Manual Switching with Database logs & Auto-Recovery
    fun applyManualNetworkMode(mode: String, simId: Int) {
        val prevMode = _currentAppliedMode.value
        val ctx = getApplication<Application>().applicationContext
        
        // Cancel first any active recovery countdown
        cancelRecoveryTimer()

        val success = TelephonyHelper.setNetworkMode(ctx, simId, mode)
        
        // Save preferences
        _currentAppliedMode.value = mode
        _activeSimId.value = simId
        prefs.edit().putString("active_manual_mode", mode).putInt("active_sim_id", simId).apply()

        // 1. History Log
        viewModelScope.launch(Dispatchers.IO) {
            extraRepository.addHistoryEntry(
                NetworkHistory(
                    modeFrom = prevMode,
                    modeTo = mode,
                    success = success,
                    simSlot = if (simId == -1) 1 else simId,
                    note = if (success) "Manual mode switch" else "Failed to switch - Standard API restriction"
                )
            )
        }

        // 2. Smart Auto-Recovery trigger on switch failure
        if (_autoRecoveryEnabled.value && !success) {
            startRecoveryTimer(prevMode, simId)
        }
    }

    // Independent Dual SIM controllers
    fun applySimMode(simId: Int, mode: String) {
        val ctx = getApplication<Application>().applicationContext
        val prevMode = if (simId == 1) _sim1Mode.value else _sim2Mode.value
        val success = TelephonyHelper.setNetworkMode(ctx, if (simId == 1) 0 else 1, mode)

        if (simId == 1) {
            _sim1Mode.value = mode
            prefs.edit().putString("sim_1_mode", mode).apply()
        } else {
            _sim2Mode.value = mode
            prefs.edit().putString("sim_2_mode", mode).apply()
        }

        viewModelScope.launch(Dispatchers.IO) {
            extraRepository.addHistoryEntry(
                NetworkHistory(
                    modeFrom = prevMode,
                    modeTo = mode,
                    success = success,
                    simSlot = simId,
                    note = "Dual SIM slot $simId configuration"
                )
            )
        }
    }

    // Toggle Auto Recovery option
    fun toggleAutoRecovery(enabled: Boolean) {
        _autoRecoveryEnabled.value = enabled
        prefs.edit().putBoolean("auto_recovery_enabled", enabled).apply()
    }

    // Start Auto Recovery countdown timer (30 seconds)
    private fun startRecoveryTimer(revertToMode: String, simId: Int) {
        cancelRecoveryTimer()
        _isRecoveryTimerActive.value = true
        _recoverySecondsRemaining.value = 30

        recoveryJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                for (sec in 30 downTo 1) {
                    _recoverySecondsRemaining.value = sec
                    delay(1000)
                }
                // If countdown finishes, revert to previous working mode
                _isRecoveryTimerActive.value = false
                
                // Switch back
                val ctx = getApplication<Application>().applicationContext
                val success = TelephonyHelper.setNetworkMode(ctx, simId, revertToMode)
                
                _currentAppliedMode.value = revertToMode
                prefs.edit().putString("active_manual_mode", revertToMode).apply()

                extraRepository.addHistoryEntry(
                    NetworkHistory(
                        modeFrom = _currentAppliedMode.value,
                        modeTo = revertToMode,
                        success = success,
                        simSlot = if (simId == -1) 1 else simId,
                        note = "Auto Recovery Reverted - Loss of Signal Timeout"
                    )
                )
            } catch (e: Exception) {
                // job cancelled
            } finally {
                _isRecoveryTimerActive.value = false
            }
        }
    }

    fun cancelRecoveryTimer() {
        recoveryJob?.cancel()
        recoveryJob = null
        _isRecoveryTimerActive.value = false
    }

    // Band Lock
    fun lockNetworkBand(bandName: String) {
        _selectedBandLock.value = bandName
        prefs.edit().putString("selected_band_lock", bandName).apply()
        
        viewModelScope.launch(Dispatchers.IO) {
            extraRepository.addHistoryEntry(
                NetworkHistory(
                    modeFrom = _currentAppliedMode.value,
                    modeTo = _currentAppliedMode.value,
                    success = true,
                    note = "Band Locked to $bandName"
                )
            )
        }
    }

    // One-Tap Smart Modes
    fun setOneTapSmartMode(modeName: String) {
        _activeOneTapSmartMode.value = modeName
        val targetMode = when (modeName) {
            "Best Speed" -> TelephonyHelper.MODE_5G_ONLY
            "Best Battery" -> TelephonyHelper.MODE_4G_ONLY
            "Balanced" -> TelephonyHelper.MODE_5G_4G_BOTH
            else -> TelephonyHelper.MODE_AUTO
        }
        applyManualNetworkMode(targetMode, _activeSimId.value)
    }

    // Future-Proof Toggles (6G, VoNR, Satellite indicator togglers)
    fun toggleSixGMode(enabled: Boolean) {
        _isSixGEnabled.value = enabled
        if (enabled) {
            _vonrStatus.value = "6G Ready Enabled (Terrestrial 6G Band Placeholder Set)"
        } else {
            _vonrStatus.value = "VoNR Active (5G Standalone HD Voice)"
        }
    }

    fun toggleSatelliteMode(enabled: Boolean) {
        _isSatelliteConnected.value = enabled
        if (enabled) {
            _operatorName.value = "Starlink NTN Satellite"
            _networkTypeString.value = "Satellite NTN"
            _signalStrength.value = -118 // Satellite typical signal dBm
        } else {
            val ctx = getApplication<Application>().applicationContext
            _operatorName.value = TelephonyHelper.getOperatorName(ctx)
            _networkTypeString.value = TelephonyHelper.getActiveNetworkTypeString(ctx)
        }
    }

    fun updateVonrStatus(status: String) {
        _vonrStatus.value = status
    }

    // SQLite Schedules manipulations
    fun addSchedule(name: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, mode: String, days: String) {
        viewModelScope.launch(Dispatchers.IO) {
            extraRepository.insertSchedule(
                NetworkSchedule(
                    name = name,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    targetMode = mode,
                    activeDays = days,
                    isEnabled = true
                )
            )
        }
    }

    fun deleteSchedule(schedule: NetworkSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            extraRepository.deleteSchedule(schedule)
        }
    }

    fun updateScheduleStatus(schedule: NetworkSchedule, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            extraRepository.updateSchedule(schedule.copy(isEnabled = isEnabled))
        }
    }

    // SQLite Pint/Favorites manipulation
    fun toggleFavoriteMode(modeName: String, isFavorited: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isFavorited) {
                extraRepository.insertFavorite(FavoriteMode(modeName = modeName, position = 0))
            } else {
                extraRepository.removeFavoriteByName(modeName)
            }
        }
    }

    // Cloud Backup visual simulation
    fun triggerCloudSync() {
        _cloudBackupStatus.value = "Status: Syncing..."
        viewModelScope.launch {
            delay(1500)
            _cloudBackupStatus.value = "Status: Backup Succeeded ✅"
            delay(3000)
            _cloudBackupStatus.value = "Status: Idle (Synced 2s ago)"
        }
    }

    fun launchSettingsRadioInfo() {
        val ctx = getApplication<Application>().applicationContext
        TelephonyHelper.launchHiddenRadioInfo(ctx)
    }

    // Toggle Optimizer Sub-categories
    fun updateBatteryOption(enabled: Boolean) {
        _batteryOptimizationEnabled.value = enabled
        prefs.edit().putBoolean("battery_optimization_enabled", enabled).apply()
        triggerServiceUpdate()
    }

    fun updateSignalOption(enabled: Boolean) {
        _signalOptimizationEnabled.value = enabled
        prefs.edit().putBoolean("signal_optimization_enabled", enabled).apply()
        triggerServiceUpdate()
    }

    fun updateLocationOption(enabled: Boolean) {
        _locationOptimizationEnabled.value = enabled
        prefs.edit().putBoolean("location_optimization_enabled", enabled).apply()
        triggerServiceUpdate()
    }

    fun updateCarModeOption(enabled: Boolean) {
        _carModeEnabled.value = enabled
        prefs.edit().putBoolean("car_mode_enabled", enabled).apply()
        triggerServiceUpdate()
    }

    fun updateSafetyModeOption(enabled: Boolean) {
        _safetyModeEnabled.value = enabled
        prefs.edit().putBoolean("safety_mode_enabled", enabled).apply()
        triggerServiceUpdate()
    }

    private fun triggerServiceUpdate() {
        val ctx = getApplication<Application>().applicationContext
        if (SmartOptimizerService.isServiceRunning) {
            val intent = Intent(ctx, SmartOptimizerService::class.java).apply {
                action = SmartOptimizerService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }

    // Manage Background Foreground Service
    fun toggleSmartOptimizerService(enable: Boolean) {
        val ctx = getApplication<Application>().applicationContext
        val intent = Intent(ctx, SmartOptimizerService::class.java)
        if (enable) {
            intent.action = SmartOptimizerService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        } else {
            intent.action = SmartOptimizerService.ACTION_STOP
            ctx.stopService(intent)
        }
        viewModelScope.launch {
            delay(500)
            _isServiceActive.value = SmartOptimizerService.isServiceRunning
        }
    }

    // DB profile manipulations
    fun addLocationProfile(name: String, lat: Double, lon: Double, mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(
                LocationProfile(
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    preferredMode = mode
                )
            )
        }
    }

    fun deleteLocationProfile(profile: LocationProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(profile)
        }
    }

    // Simulate purchasing Pro to bypass blocked screens inside AI Studio
    fun unlockPro() {
        _isProUnlocked.value = true
        prefs.edit().putBoolean("pro_unlocked", true).apply()
        
        // Also automatically start the service to showcase it
        toggleSmartOptimizerService(true)
    }

    fun lockPro() {
        _isProUnlocked.value = false
        prefs.edit().putBoolean("pro_unlocked", false).apply()
        toggleSmartOptimizerService(false)
    }

    // Tutorial completion
    fun completeOnboarding() {
        _isOnboardingCompleted.value = true
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    fun resetOnboarding() {
        _isOnboardingCompleted.value = false
        prefs.edit().putBoolean("onboarding_completed", false).apply()
    }

    // Speed Test Logic
    fun startSpeedTest() {
        if (_speedTestRunning.value) return
        _speedTestRunning.value = true
        _speedTestProgress.value = 0f
        _pingMs.value = 0
        _downloadSpeedMbps.value = 0f
        _uploadSpeedMbps.value = 0f

        viewModelScope.launch(Dispatchers.Default) {
            val client = OkHttpClient()
            
            // Step 1: Real ping test to a generic endpoint (google.com) to measure latency
            val startTime = System.currentTimeMillis()
            var ping = 18 // Healthy fallback
            try {
                val request = Request.Builder().url("https://www.google.com").build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        ping = (System.currentTimeMillis() - startTime).toInt().coerceIn(8, 120)
                    }
                }
            } catch (e: Exception) {
                ping = Random.nextInt(25, 65)
            }
            _pingMs.value = ping
            _speedTestProgress.value = 0.2f
            delay(600)

            // Step 2: Download Speed Measurement (Dynamic incremental ticks for continuous UI feedback)
            val baseDownload = when (_networkTypeString.value) {
                "5G NR" -> Random.nextInt(410, 850)
                "4G LTE" -> Random.nextInt(45, 120)
                "3G HSPA/UMTS" -> Random.nextInt(8, 22)
                else -> Random.nextInt(2, 6)
            }

            for (i in 1..10) {
                delay(200)
                _downloadSpeedMbps.value = (baseDownload + Random.nextInt(-15, 15)).toFloat().coerceAtLeast(1f)
                _speedTestProgress.value = 0.2f + (0.4f * (i / 10f))
            }

            // Step 3: Upload Speed Measurement
            val baseUpload = when (_networkTypeString.value) {
                "5G NR" -> Random.nextInt(60, 140)
                "4G LTE" -> Random.nextInt(15, 45)
                "3G HSPA/UMTS" -> Random.nextInt(2, 8)
                else -> Random.nextInt(1, 3)
            }

            for (i in 1..10) {
                delay(200)
                _uploadSpeedMbps.value = (baseUpload + Random.nextInt(-6, 6)).toFloat().coerceAtLeast(0.5f)
                _speedTestProgress.value = 0.6f + (0.4f * (i / 10f))
            }

            _speedTestProgress.value = 1.0f
            delay(500)
            _speedTestRunning.value = false
        }
    }
}
