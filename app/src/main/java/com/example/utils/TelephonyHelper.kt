package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoGsm
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import java.lang.reflect.Method

object TelephonyHelper {
    private const val TAG = "TelephonyHelper"

    // Network Mode Bitmasks for API 30+ setAllowedNetworkTypesForReason
    private const val BITMASK_2G = (1 shl TelephonyManager.NETWORK_TYPE_GPRS) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_EDGE) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_CDMA) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_1xRTT) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_IDEN)

    private const val BITMASK_3G = (1 shl TelephonyManager.NETWORK_TYPE_UMTS) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_EVDO_0) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_EVDO_A) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_HSDPA) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_HSUPA) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_HSPA) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_EVDO_B) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_EHRPD) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_HSPAP) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_TD_SCDMA)

    private const val BITMASK_4G = (1 shl TelephonyManager.NETWORK_TYPE_LTE) or 
                           (1 shl TelephonyManager.NETWORK_TYPE_IWLAN)

    private const val BITMASK_5G = (1 shl TelephonyManager.NETWORK_TYPE_NR)

    private const val BITMASK_5G_4G = BITMASK_4G or BITMASK_5G

    // Combined bitmasks
    private const val BITMASK_AUTO = BITMASK_2G or BITMASK_3G or BITMASK_4G or BITMASK_5G

    // Network Mode constants representing our UI selections
    const val MODE_5G_ONLY = "5G Only"
    const val MODE_4G_ONLY = "4G Only"
    const val MODE_5G_4G_BOTH = "5G/4G Mode"
    const val MODE_3G_ONLY = "3G Only"
    const val MODE_2G_ONLY = "2G Only"
    const val MODE_AUTO = "Auto Mode"

    /**
     * Gets active SIM cards using SubscriptionManager
     */
    fun getActiveSimCount(context: Context): Int {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeList = sm.activeSubscriptionInfoList
            activeList?.size ?: 1
        } catch (e: SecurityException) {
            1
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Gets a list of SIM slot indices / subscription IDs
     */
    fun getActiveSubscriptions(context: Context): List<Pair<Int, String>> {
        val list = mutableListOf<Pair<Int, String>>()
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeList = sm.activeSubscriptionInfoList
            if (activeList != null) {
                for (info in activeList) {
                    list.add(Pair(info.subscriptionId, "SIM ${info.simSlotIndex + 1} (${info.displayName})"))
                }
            }
        } catch (e: Exception) {
            list.add(Pair(-1, "SIM 1 (Default)"))
        }
        if (list.isEmpty()) {
            list.add(Pair(-1, "SIM 1 (Default)"))
        }
        return list
    }

    /**
     * Attempts to switch the network mode on a specific subscription ID
     */
    fun setNetworkMode(context: Context, subId: Int, mode: String): Boolean {
        Log.d(TAG, "Attempting to set subscription $subId to mode $mode")
        
        // 1. First Attempt: Modern Android API (API 30+) setAllowedNetworkTypesForReason
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val subTm = if (subId != -1) tm.createForSubscriptionId(subId) else tm
                
                val bitmask = when (mode) {
                    MODE_5G_ONLY -> BITMASK_5G
                    MODE_4G_ONLY -> BITMASK_4G
                    MODE_5G_4G_BOTH -> BITMASK_5G_4G
                    MODE_3G_ONLY -> BITMASK_3G
                    MODE_2G_ONLY -> BITMASK_2G
                    else -> BITMASK_AUTO
                }
                
                // Allowed reason parameter is ALLOWED_NETWORK_TYPES_REASON_USER
                subTm.setAllowedNetworkTypesForReason(
                    TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER,
                    bitmask.toLong()
                )
                Log.d(TAG, "Successfully set network mode via setAllowedNetworkTypesForReason (API 30+)")
                return true
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException in API 30+ call: ${e.message}. Will try reflection.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception in API 30+ call: ${e.message}. Will try reflection.")
            }
        }

        // 2. Second Attempt: Reflection API calling internal `setPreferredNetworkType` or hidden APIs
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subTm = if (subId != -1) tm.createForSubscriptionId(subId) else tm
            
            // Hidden network mode integers:
            // NT_MODE_NR_ONLY = 26
            // NT_MODE_LTE_ONLY = 11
            // NT_MODE_WCDMA_ONLY = 2
            // NT_MODE_GSM_ONLY = 1
            // NT_MODE_LTE_WCDMA_GSM_EVDO_CDMA_AND_EVERYTHING_AUTO = 22
            val typeInt = when (mode) {
                MODE_5G_ONLY -> 26
                MODE_4G_ONLY -> 11
                MODE_5G_4G_BOTH -> 25
                MODE_3G_ONLY -> 2
                MODE_2G_ONLY -> 1
                else -> 22
            }

            // Look for hidden write methods like setPreferredNetworkType
            val setPreferredMethod: Method = subTm.javaClass.getMethod(
                "setPreferredNetworkType", 
                Int::class.javaPrimitiveType
            )
            setPreferredMethod.isAccessible = true
            val success = setPreferredMethod.invoke(subTm, typeInt) as Boolean
            if (success) {
                Log.d(TAG, "Successfully set network mode via reflection setPreferredNetworkType(Int)")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reflection for setPreferredNetworkType(Int) failed: ${e.message}")
        }

        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subTm = if (subId != -1) tm.createForSubscriptionId(subId) else tm
            
            val typeInt = when (mode) {
                MODE_5G_ONLY -> 26
                MODE_4G_ONLY -> 11
                MODE_5G_4G_BOTH -> 25
                MODE_3G_ONLY -> 2
                MODE_2G_ONLY -> 1
                else -> 22
            }

            // Try the long variant setPreferredNetworkType(subId, type)
            val setPreferredWithSubMethod: Method = tm.javaClass.getMethod(
                "setPreferredNetworkType",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            setPreferredWithSubMethod.isAccessible = true
            val success = setPreferredWithSubMethod.invoke(tm, subId, typeInt) as Boolean
            if (success) {
                Log.d(TAG, "Successfully set network mode via reflection setPreferredNetworkType(subId, type)")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reflection for setPreferredNetworkType(subId, type) failed: ${e.message}")
        }

        // 3. Shizuku API simulation / fallback notice
        Log.w(TAG, "Switch failed. Standard device APIs are locked. Fall back to manual launcher.")
        return false
    }

    /**
     * Direct launch of standard hidden Phone Testing/RadioInfo Settings page
     */
    fun launchHiddenRadioInfo(context: Context): Boolean {
        val intents = listOf(
            Intent().setClassName("com.android.settings", "com.android.settings.RadioInfo"),
            Intent().setClassName("com.android.settings", "com.android.settings.BandMode"),
            Intent("android.intent.action.MAIN").setClassName("com.android.settings", "com.android.settings.RadioInfo"),
            Intent("android.settings.BAND_MODE_SETTINGS")
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Ignore and try next
            }
        }
        return false
    }

    /**
     * Gets active network type name for display: "5G", "4G", "3G", "2G" or "Unknown" / "WiFi"
     */
    @SuppressLint("MissingPermission")
    fun getActiveNetworkTypeString(context: Context): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork ?: return "No Connection"
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "No Connection"
            
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "Wi-Fi Connected"
            }
            
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tm.dataNetworkType
            } else {
                @Suppress("DEPRECATION")
                tm.networkType
            }
            
            return when (networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_LTE, 
                TelephonyManager.NETWORK_TYPE_IWLAN -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_UMTS, 
                TelephonyManager.NETWORK_TYPE_HSDPA, 
                TelephonyManager.NETWORK_TYPE_HSUPA, 
                TelephonyManager.NETWORK_TYPE_HSPA, 
                TelephonyManager.NETWORK_TYPE_HSPAP, 
                TelephonyManager.NETWORK_TYPE_EHRPD, 
                TelephonyManager.NETWORK_TYPE_EVDO_0, 
                TelephonyManager.NETWORK_TYPE_EVDO_A, 
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G HSPA/UMTS"
                TelephonyManager.NETWORK_TYPE_GPRS, 
                TelephonyManager.NETWORK_TYPE_EDGE, 
                TelephonyManager.NETWORK_TYPE_CDMA, 
                TelephonyManager.NETWORK_TYPE_1xRTT, 
                TelephonyManager.NETWORK_TYPE_IDEN -> "2G EDGE/GSM"
                else -> "Active (4G/5G Auto)"
            }
        } catch (e: Exception) {
            return "Cellular Mode"
        }
    }

    /**
     * Measures cellular signal strength in dBm, falling back dynamically on cell structures
     */
    @SuppressLint("MissingPermission")
    fun getSignalStrengthDbm(context: Context): Int {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val cellInfos: List<CellInfo>? = tm.allCellInfo
            if (cellInfos.isNullOrEmpty()) {
                return -95 // Reasonable default fallback if cellular scanning permission is denied or zero cells available
            }
            
            for (info in cellInfos) {
                if (info.isRegistered) {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr -> {
                            val dbm = info.cellSignalStrength.dbm
                            if (dbm != CellInfo.UNAVAILABLE) return dbm
                        }
                        info is CellInfoLte -> {
                            val dbm = info.cellSignalStrength.dbm
                            if (dbm != CellInfo.UNAVAILABLE) return dbm
                        }
                        info is CellInfoWcdma -> {
                            val dbm = info.cellSignalStrength.dbm
                            if (dbm != CellInfo.UNAVAILABLE) return dbm
                        }
                        info is CellInfoGsm -> {
                            val dbm = info.cellSignalStrength.dbm
                            if (dbm != CellInfo.UNAVAILABLE) return dbm
                        }
                    }
                }
            }
            // fallback if registered is not accessible but we have first available cell's dbm
            val first = cellInfos.firstOrNull() ?: return -95
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && first is CellInfoNr -> {
                    val dbm = first.cellSignalStrength.dbm
                    if (dbm != CellInfo.UNAVAILABLE) return dbm
                }
                first is CellInfoLte -> {
                    val dbm = first.cellSignalStrength.dbm
                    if (dbm != CellInfo.UNAVAILABLE) return dbm
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error measuring signal strength: ${e.message}")
        }
        return -92 // Decent offline default
    }

    /**
     * Gets the carrier operator name
     */
    fun getOperatorName(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val name = tm.networkOperatorName
            if (name.isNullOrBlank()) "No Carrier" else name
        } catch (e: Exception) {
            "Carrier"
        }
    }
}
