package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.utils.TelephonyHelper

@RequiresApi(Build.VERSION_CODES.N)
class QuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        // Check if PRO is unlocked
        val prefs = getSharedPreferences("smart_optimizer_prefs", Context.MODE_PRIVATE)
        val isProUnlocked = prefs.getBoolean("pro_unlocked", false)
        
        if (!isProUnlocked) {
            Toast.makeText(this, "Quick Settings controls require Pro Version!", Toast.LENGTH_LONG).show()
            
            // Route to MainActivity so user can unlock
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ start activity from QS expects pending intent or standard starting. we run safely
                try {
                    startActivityAndCollapse(intent)
                } catch (e: Exception) {
                    startActivity(intent)
                }
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        val currentMode = prefs.getString("active_manual_mode", TelephonyHelper.MODE_AUTO) ?: TelephonyHelper.MODE_AUTO
        
        // Cycle mode: Auto -> 5G Only -> 4G Only -> 5G/4G Mode -> Auto
        val nextMode = when (currentMode) {
            TelephonyHelper.MODE_AUTO -> TelephonyHelper.MODE_5G_ONLY
            TelephonyHelper.MODE_5G_ONLY -> TelephonyHelper.MODE_4G_ONLY
            TelephonyHelper.MODE_4G_ONLY -> TelephonyHelper.MODE_5G_4G_BOTH
            else -> TelephonyHelper.MODE_AUTO
        }

        // Apply
        val success = TelephonyHelper.setNetworkMode(this, -1, nextMode)
        if (success) {
            prefs.edit().putString("active_manual_mode", nextMode).apply()
            Toast.makeText(this, "Toggled Mode: $nextMode", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "API blocked. Opening hidden settings.", Toast.LENGTH_SHORT).show()
            TelephonyHelper.launchHiddenRadioInfo(this)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("smart_optimizer_prefs", Context.MODE_PRIVATE)
        val currentMode = prefs.getString("active_manual_mode", TelephonyHelper.MODE_AUTO) ?: TelephonyHelper.MODE_AUTO
        val isProUnlocked = prefs.getBoolean("pro_unlocked", false)

        if (!isProUnlocked) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "5G Opt (PRO)"
            tile.subtitle = "Locked"
        } else {
            tile.label = "5G Optimizer"
            tile.subtitle = currentMode
            tile.state = when (currentMode) {
                TelephonyHelper.MODE_AUTO -> Tile.STATE_INACTIVE
                else -> Tile.STATE_ACTIVE
            }
        }
        tile.updateTile()
    }
}
