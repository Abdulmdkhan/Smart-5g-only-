package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.*
import com.example.data.LocationProfile
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.TelephonyHelper
import com.example.viewmodel.NetworkViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: NetworkViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[NetworkViewModel::class.java]

        setContent {
            MyApplicationTheme(darkTheme = true) { // Force beautiful Premium Dark Mode
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F1115) // Solid premium deep carbon space background
                ) {
                    if (!isOnboardingCompleted) {
                        OnboardingTutorialScreen(onCompleted = { viewModel.completeOnboarding() })
                    } else {
                        MainAppNavigation(viewModel)
                    }
                }
            }
        }
    }
}

// 1. Sleek Interactive Tutorial Screen (Onboarding)
@Composable
fun OnboardingTutorialScreen(onCompleted: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    
    val pulseScale by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .navigationBarsPadding()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NetworkCell,
                contentDescription = null,
                tint = Color(0xFF10B981), // Neon 5G Emerald Green
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .padding(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "5G SMART SWITCHER",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Power-user Network Optimizer & Auto-Switch",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        // Stepper Content
        Crossfade(targetState = step, label = "") { currentStep ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentStep) {
                    1 -> {
                        Text(
                            text = "⚡ Force NR / 5G Only",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Standard Android menus hide the 5G Only option, forcing a drop to 4G in minor conditions. Our app unlocks preferred bands using deep internal Reflection APIs, letting you lock 5G with maximum stability.",
                            fontSize = 14.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                    2 -> {
                        Text(
                            text = "🔋 Smart Background Optimizer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF6366F1)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Continuous 5G sweeps consume 25%+ more battery. The Smart Optimizer monitors battery and background signals to dynamically switch back to 4G when battery falls below 15% or signals degrade.",
                            fontSize = 14.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                    3 -> {
                        Text(
                            text = "🛠️ Power-User / Shizuku Fallbacks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Because manufacturers block cellular overrides, some networks restrict API access. We offer a direct 1-click bypass, Shizuku shell bindings, or ADB tutorials to keep settings accessible on all devices.",
                            fontSize = 14.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Step tracker visual dot indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            for (i in 1..3) {
                val color = if (i == step) Color(0xFF10B981) else Color.White.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .size(width = if (i == step) 24.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Action controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                TextButton(onClick = { step-- }) {
                    Text("Back", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(modifier = Modifier.width(60.dp))
            }

            Button(
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        onCompleted()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (step == 3) Color(0xFF10B981) else Color(0xFF1E293B)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (step == 3) "Enable & Proceed" else "Next",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 2. Main Navigation Shell Setup
@Composable
fun MainAppNavigation(viewModel: NetworkViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF151821),
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard", fontSize = 10.sp) },
                    selected = currentRoute == "dashboard",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    label = { Text("Optimizer", fontSize = 10.sp) },
                    selected = currentRoute == "optimizer",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    onClick = {
                        navController.navigate("optimizer") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Geo Profiles", fontSize = 10.sp) },
                    selected = currentRoute == "profiles",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    onClick = {
                        navController.navigate("profiles") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                    label = { Text("Help", fontSize = 10.sp) },
                    selected = currentRoute == "help",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    onClick = {
                        navController.navigate("help") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") { DashboardTab(viewModel) }
            composable("optimizer") { OptimizerTab(viewModel) }
            composable("profiles") { ProfilesTab(viewModel) }
            composable("help") { HelpTab(viewModel) }
        }
    }
}

// 3. TAB 1: Dashboard with live signal graph & speed tests
@Composable
fun DashboardTab(viewModel: NetworkViewModel) {
    val context = LocalContext.current
    val signalStrength by viewModel.signalStrength.collectAsState()
    val networkTypeString by viewModel.networkTypeString.collectAsState()
    val operatorName by viewModel.operatorName.collectAsState()
    val currentAppliedMode by viewModel.currentAppliedMode.collectAsState()
    val activeSimId by viewModel.activeSimId.collectAsState()

    // EXTRA FEATURE FLOWS FOR DASHBOARD TAB
    val networkHistory by viewModel.networkHistory.collectAsState()
    val allSchedules by viewModel.allSchedules.collectAsState()
    val favoriteModes by viewModel.favoriteModes.collectAsState()
    val isSatelliteConnected by viewModel.isSatelliteConnected.collectAsState()
    val isSixGEnabled by viewModel.isSixGEnabled.collectAsState()
    val vonrStatus by viewModel.vonrStatus.collectAsState()
    val selectedBandLock by viewModel.selectedBandLock.collectAsState()
    val autoRecoveryEnabled by viewModel.autoRecoveryEnabled.collectAsState()
    val isRecoveryTimerActive by viewModel.isRecoveryTimerActive.collectAsState()
    val recoverySecondsRemaining by viewModel.recoverySecondsRemaining.collectAsState()
    val cloudBackupStatus by viewModel.cloudBackupStatus.collectAsState()
    val activeOneTapSmartMode by viewModel.activeOneTapSmartMode.collectAsState()
    val sim1Mode by viewModel.sim1Mode.collectAsState()
    val sim2Mode by viewModel.sim2Mode.collectAsState()

    // Sliding historical signal strength values
    val signalHistory = remember { mutableStateListOf<Float>() }
    LaunchedEffect(signalStrength) {
        signalHistory.add(signalStrength.toFloat())
        if (signalHistory.size > 22) {
            signalHistory.removeAt(0)
        }
    }

    var selectedSim by remember { mutableStateOf(1) } // Default SIM Slot selection
    var permissionGranted by remember { mutableStateOf(false) }

    // Multi-permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val phoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (!fineLocation || !phoneState) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        } else {
            permissionGranted = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Smart Auto Recovery Countdown Visual Alert Panel
        if (isRecoveryTimerActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Speed Loss Auto-Recovery Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                            Text("No connection verified. Reverting to working state in $recoverySecondsRemaining s", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.cancelRecoveryTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // App Title & Current Carrier Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = operatorName.uppercase(),
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "State: $networkTypeString",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (networkTypeString.contains("5G")) "5G ACTIVE" else "LTE ACTIVE",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Live Signal Circular Gauge Canvas & Sliding History Wave
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Real-time Signal Analysis",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Dynamic Circular Gauge Painting
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        Canvas(modifier = Modifier.size(175.dp)) {
                            // Scale from -135dBm to -45dBm
                            val percent = ((signalStrength - (-135f)) / 90f).coerceIn(0f, 1f)
                            
                            // Background track arc
                            drawArc(
                                color = Color.Gray.copy(alpha = 0.15f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Signal Sweep Brush Gradation
                            val gradientBrush = Brush.sweepGradient(
                                colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981))
                            )

                            drawArc(
                                brush = gradientBrush,
                                startAngle = 135f,
                                sweepAngle = 270f * percent,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$signalStrength",
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "dBm",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = when {
                                    signalStrength > -80 -> "Excellent Quality"
                                    signalStrength > -100 -> "Stable Connection"
                                    else -> "Weak / Poor Band"
                                },
                                color = if (signalStrength > -90) Color(0xFF10B981) else Color(0xFFF97316),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. Premium Signal History Sliding Graph Canvas
                    Text(
                        text = "Historical Pulse Frequency",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color(0xFF111317), shape = RoundedCornerShape(8.dp))
                    ) {
                        if (signalHistory.isNotEmpty()) {
                            val path = Path()
                            val widthInterval = size.width / 21f
                            
                            // Scale values normalized to size.height
                            val mappedPoints = signalHistory.map { dbm ->
                                val pct = ((dbm - (-135f)) / 90f).coerceIn(0f, 1f)
                                size.height - (pct * (size.height - 12f) + 6f)
                            }

                            path.moveTo(0f, mappedPoints.first())
                            for (index in 1 until mappedPoints.size) {
                                path.lineTo(index * widthInterval, mappedPoints[index])
                            }

                            // Outline stroke
                            drawPath(
                                path = path,
                                color = Color(0xFF10B981),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }

         // 1. One-Tap Smart Modes with Adaptive AI Insight
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adaptive One-Tap Smart Presets",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("AI ACTIVE", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Best Speed", "Best Battery", "Balanced").forEach { smMode ->
                            val isSelected = activeOneTapSmartMode == smMode
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF10B981) else Color(0xFF242936))
                                    .clickable { viewModel.setOneTapSmartMode(smMode) }
                                    .padding(vertical = 11.dp)
                            ) {
                                Text(
                                    text = smMode,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Adaptive intelligence suggestions box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111317), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkWifi,
                            contentDescription = "AI",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                signalStrength > -75 -> "Core Recommendation: Excellent coverage detected. Enjoy raw high-speed '5G Only' bandwidth!"
                                signalStrength < -100 -> "Core Recommendation: Extremely weak transceiver detected. Reverting to 'Balanced' to mitigate continuous modem straining & saving up to 18% thermal battery load."
                                else -> "Core Recommendation: Steady carrier power discovered. Utilizing 'Balanced' mode to save 12% device radio power."
                            },
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 2. Enhanced Dual SIM Configuration & Transceiver Slots
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Enhanced Dual SIM Multi-Transceiver",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize individual target restrictions for cellular interfaces independently.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // SIM Slot 1 Configuration card
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF111317), shape = RoundedCornerShape(12.dp))
                                .clickable { selectedSim = 1 }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = null,
                                        tint = if (selectedSim == 1) Color(0xFF10B981) else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SIM slot 1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                if (selectedSim == 1) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF10B981), shape = CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Applied: $sim1Mode", color = Color.Gray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { viewModel.applySimMode(1, TelephonyHelper.MODE_5G_ONLY) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242936)),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.weight(1f).height(24.dp)
                                ) {
                                    Text("5G Only", color = Color.White, fontSize = 9.sp)
                                }
                                Button(
                                    onClick = { viewModel.applySimMode(1, TelephonyHelper.MODE_4G_ONLY) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242936)),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.weight(1f).height(24.dp)
                                ) {
                                    Text("4G Only", color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }

                        // SIM Slot 2 Configuration card
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF111317), shape = RoundedCornerShape(12.dp))
                                .clickable { selectedSim = 2 }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = null,
                                        tint = if (selectedSim == 2) Color(0xFF10B981) else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SIM slot 2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                if (selectedSim == 2) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF10B981), shape = CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Applied: $sim2Mode", color = Color.Gray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { viewModel.applySimMode(2, TelephonyHelper.MODE_5G_ONLY) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242936)),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.weight(1f).height(24.dp)
                                ) {
                                    Text("5G Only", color = Color.White, fontSize = 9.sp)
                                }
                                Button(
                                    onClick = { viewModel.applySimMode(2, TelephonyHelper.MODE_4G_ONLY) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242936)),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.weight(1f).height(24.dp)
                                ) {
                                    Text("4G Only", color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Preferred Network Mode & Star Pins
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Hardware Band Locking Priorities",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    val favoritesList = favoriteModes.map { it.modeName }
                    if (favoritesList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Pinned Fast Access Favorites",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            favoritesList.forEach { favMode ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                        .clickable { 
                                            viewModel.applyManualNetworkMode(favMode, if (selectedSim == 1) -1 else 2)
                                            Toast.makeText(context, "$favMode applied", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(favMode, color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val modes = listOf(
                        TelephonyHelper.MODE_5G_ONLY,
                        TelephonyHelper.MODE_4G_ONLY,
                        TelephonyHelper.MODE_5G_4G_BOTH,
                        TelephonyHelper.MODE_3G_ONLY,
                        TelephonyHelper.MODE_2G_ONLY,
                        TelephonyHelper.MODE_AUTO
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { targetMode ->
                            val isCurrent = currentAppliedMode == targetMode
                            val isPinned = favoritesList.contains(targetMode)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFF242936))
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.applyManualNetworkMode(targetMode, if (selectedSim == 1) -1 else 2)
                                            Toast.makeText(context, "$targetMode locked for SIM $selectedSim", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Icon(
                                        imageVector = when (targetMode) {
                                            TelephonyHelper.MODE_5G_ONLY -> Icons.Default.SignalCellularConnectedNoInternet4Bar
                                            TelephonyHelper.MODE_4G_ONLY -> Icons.Default.NetworkCell
                                            TelephonyHelper.MODE_5G_4G_BOTH -> Icons.Default.SignalCellularAlt
                                            else -> Icons.Default.SignalCellular4Bar
                                        },
                                        contentDescription = null,
                                        tint = if (isCurrent) Color(0xFF10B981) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = targetMode,
                                        color = if (isCurrent) Color(0xFF10B981) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.toggleFavoriteMode(targetMode, !isPinned) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Pin Mode",
                                            tint = if (isPinned) Color(0xFFF59E0B) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.launchSettingsRadioInfo() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_radio_info_btn")
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bypass: Launch Hidden Service RadioInfo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Custom RF Hardware Overrides (Band locks)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Specific RF Transceiver Band Lock",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Attempting to lock the baseband hardware exclusively to a carrier-specific channel block.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val bands = listOf("5G n78 (3.5 GHz)", "5G n258 (mmWave)", "LTE B3 (1800 MHz)", "LTE B7 (2600 MHz)")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            bands.take(2).forEach { band ->
                                val isLocked = selectedBandLock == band
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isLocked) Color(0xFFE11D48).copy(alpha = 0.22f) else Color(0xFF242936))
                                        .clickable { viewModel.lockNetworkBand(band) }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(band, color = if (isLocked) Color(0xFFE11D48) else Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            bands.drop(2).forEach { band ->
                                val isLocked = selectedBandLock == band
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isLocked) Color(0xFFE11D48).copy(alpha = 0.22f) else Color(0xFF242936))
                                        .clickable { viewModel.lockNetworkBand(band) }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(band, color = if (isLocked) Color(0xFFE11D48) else Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Intelligent Time-based Rules & Automation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                var showAddDialog by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Automatic Schedules & Rules", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Trigger frequency locks based on timing", color = Color.Gray, fontSize = 11.sp)
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = Color(0xFF10B981))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    if (allSchedules.isEmpty()) {
                        Text(
                            text = "No schedules created yet. Create rules to transition bands safely during work hours.",
                            color = Color.DarkGray,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            allSchedules.forEach { s ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF111317), shape = RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "Target Mode: ${s.targetMode} from %02d:%02d to %02d:%02d".format(s.startHour, s.startMinute, s.endHour, s.endMinute),
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = s.isEnabled,
                                            onCheckedChange = { viewModel.updateScheduleStatus(s, it) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                                        )
                                        IconButton(onClick = { viewModel.deleteSchedule(s) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.70f), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (showAddDialog) {
                    var sName by remember { mutableStateOf("") }
                    var sStartHour by remember { mutableStateOf("09") }
                    var sStartMin by remember { mutableStateOf("00") }
                    var sEndHour by remember { mutableStateOf("17") }
                    var sEndMin by remember { mutableStateOf("00") }
                    var sTargetMode by remember { mutableStateOf(TelephonyHelper.MODE_5G_ONLY) }
                    
                    AlertDialog(
                        onDismissRequest = { showAddDialog = false },
                        containerColor = Color(0xFF1E2230),
                        title = { Text("Create Schedulers Rule", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = sName,
                                    onValueChange = { sName = it },
                                    label = { Text("Task Label, e.g. Night Safe Mode", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = sStartHour,
                                        onValueChange = { sStartHour = it },
                                        label = { Text("Start Hour") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    OutlinedTextField(
                                        value = sStartMin,
                                        onValueChange = { sStartMin = it },
                                        label = { Text("Min") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = sEndHour,
                                        onValueChange = { sEndHour = it },
                                        label = { Text("End Hour") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    OutlinedTextField(
                                        value = sEndMin,
                                        onValueChange = { sEndMin = it },
                                        label = { Text("Min") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                                
                                Text("Automated Mode Target:", color = Color.Gray, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(TelephonyHelper.MODE_5G_ONLY, TelephonyHelper.MODE_4G_ONLY).forEach { mode ->
                                        val isSel = sTargetMode == mode
                                        Button(
                                            onClick = { sTargetMode = mode },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isSel) Color(0xFF10B981) else Color(0xFF242936)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (mode == TelephonyHelper.MODE_5G_ONLY) "5G Only" else "4G Only", fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (sName.isNotEmpty()) {
                                        viewModel.addSchedule(
                                            name = sName,
                                            startHour = sStartHour.toIntOrNull() ?: 9,
                                            startMinute = sStartMin.toIntOrNull() ?: 0,
                                            endHour = sEndHour.toIntOrNull() ?: 17,
                                            endMinute = sEndMin.toIntOrNull() ?: 0,
                                            mode = sTargetMode,
                                            days = "everyday"
                                        )
                                    }
                                    showAddDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Save Rule", color = Color.Black)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    )
                }
            }
        }

        // 6. Future Proof Network Technologies & Cloud Sync
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Future-Proof Space Network Suite",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // 6G Ready visual toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Pre-Release 6G Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Enables conceptual Sub-THz transceiver placeholders", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = isSixGEnabled,
                            onCheckedChange = { viewModel.toggleSixGMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                        )
                    }
                    
                    // Satellite NTN visual toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sat-to-Modem routing (NTN)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Direct handoff to low-earth orbits satellite constellation", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = isSatelliteConnected,
                            onCheckedChange = { viewModel.toggleSatelliteMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 10.dp))

                    // VoNR / VoWiFi status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(vonrStatus, color = Color.LightGray, fontSize = 11.5.sp)
                    }

                    // Tasker plugin status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tasker Integration: Broadcast system receiver actively listening", color = Color.LightGray, fontSize = 11.5.sp)
                    }

                    // Cloud backup
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111317), shape = RoundedCornerShape(8.dp))
                            .clickable { viewModel.triggerCloudSync() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Pro Cloud Sync", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Backup schedules & favorite modes securely", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                        Text(cloudBackupStatus, color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 7. Local SQlite Switching Logs & History
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historical Switching Record Logs",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("SQLITE", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (networkHistory.isEmpty()) {
                        Text("No recorded switches yet. Apply a lock mode to write record.", color = Color.DarkGray, fontSize = 11.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            networkHistory.take(4).forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF111317), shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("SIM Slot ${log.simSlot}: ${log.modeFrom} ➡️ ${log.modeTo}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text(log.note, color = Color.Gray, fontSize = 9.5.sp)
                                    }
                                    Text(
                                        text = if (log.success) "Success ✅" else "Restrained ⚠️",
                                        color = if (log.success) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Small premium advertising space for security partner
        item {
            SponsoredAdCard()
        }

        // Speed Test Live Dashboard
        item {
            SpeedTestWidget(viewModel)
        }
    }
}

// Interactive Speed Test Section
@Composable
fun SpeedTestWidget(viewModel: NetworkViewModel) {
    val speedTestRunning by viewModel.speedTestRunning.collectAsState()
    val downloadSpeed by viewModel.downloadSpeedMbps.collectAsState()
    val uploadSpeed by viewModel.uploadSpeedMbps.collectAsState()
    val pingMs by viewModel.pingMs.collectAsState()
    val speedTestProgress by viewModel.speedTestProgress.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed Stream Diagnostics",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (speedTestRunning) {
                    CircularProgressIndicator(
                        progress = { speedTestProgress },
                        color = Color(0xFF10B981),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LATENCY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("$pingMs ms", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DOWNLOAD", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("${downloadSpeed.toInt()} Mbps", color = Color(0xFF10B981), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("UPLOAD", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("${uploadSpeed.toInt()} Mbps", color = Color(0xFF6366F1), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { speedTestProgress },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF10B981),
                trackColor = Color.Gray.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.startSpeedTest() },
                enabled = !speedTestRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_speedtest")
            ) {
                Text(
                    text = if (speedTestRunning) "Testing Stream Channels..." else "Start Diagnostic Speed Test",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// 4. TAB 2: Optimizer Controls & Simulated checkout
@Composable
fun OptimizerTab(viewModel: NetworkViewModel) {
    val isProUnlocked by viewModel.isProUnlocked.collectAsState()
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val serviceStatusText by viewModel.serviceStatusText.collectAsState()

    val batteryEnabled by viewModel.batteryOptimizationEnabled.collectAsState()
    val signalEnabled by viewModel.signalOptimizationEnabled.collectAsState()
    val locationEnabled by viewModel.locationOptimizationEnabled.collectAsState()
    val carModeEnabled by viewModel.carModeEnabled.collectAsState()
    val safetyModeEnabled by viewModel.safetyModeEnabled.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MONETIZATION: Simulate Premium Checkout Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isProUnlocked) Color(0xFF065F46) else Color(0xFF1E1B4B)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isProUnlocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "PRO LICENSE ACTIVE",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            "Thank you for your support! Smart switching and widgets are fully enabled.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { viewModel.lockPro() }
                        ) {
                            Text("Simulate Free Version downgrade", color = Color.White.copy(alpha = 0.6f))
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upgrade to Premium PRO",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Unlocks background smart switching routines (Battery, Signal & Geofence profiles), QS tile tracking, Home screen widgets, and priority support.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.unlockPro() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pro_purchase_btn")
                        ) {
                            Text(
                                text = "Instant Simulated Purchase - Unlock PRO",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Background service toggles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Smart Optimizer Background Core",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Auto-Optimizer", fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text(
                                "Monitors parameters continuously via foreground task",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isServiceActive,
                            enabled = isProUnlocked,
                            onCheckedChange = { viewModel.toggleSmartOptimizerService(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111317))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = serviceStatusText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (isServiceActive) Color(0xFF10B981) else Color.Gray
                        )
                    }
                }
            }
        }

        // Tuning properties
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Optimization Routine Handles",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Battery based
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery Saver Optimizer", color = Color.White, fontSize = 14.sp)
                            Text("Switches back to 4G automatically when battery drops < 15%", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = batteryEnabled,
                            enabled = isProUnlocked && isServiceActive,
                            onCheckedChange = { viewModel.updateBatteryOption(it) }
                        )
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    // Signal based
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Signal Stability Optimizer", color = Color.White, fontSize = 14.sp)
                            Text("Switches back to 4G if 5G signal dBm < -110 (poor NR coverage)", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = signalEnabled,
                            enabled = isProUnlocked && isServiceActive,
                            onCheckedChange = { viewModel.updateSignalOption(it) }
                        )
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    // Location based
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location Profile Swapper", color = Color.White, fontSize = 14.sp)
                            Text("Arriving at custom zones activates specific target settings", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = locationEnabled,
                            enabled = isProUnlocked && isServiceActive,
                            onCheckedChange = { viewModel.updateLocationOption(it) }
                        )
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    // Car mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Car Mode LTE Lock", color = Color.White, fontSize = 14.sp)
                            Text("Locks bands to 4G while driving for seamless handover transition", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = carModeEnabled,
                            enabled = isProUnlocked && isServiceActive,
                            onCheckedChange = { viewModel.updateCarModeOption(it) }
                        )
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    // Safety Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Universal Safety Fallback Mode", color = Color.White, fontSize = 14.sp)
                            Text("Ensures auto-recovery backup if custom bands ever fail", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = safetyModeEnabled,
                            enabled = isProUnlocked,
                            onCheckedChange = { viewModel.updateSafetyModeOption(it) }
                        )
                    }
                }
            }
        }
    }
}

// 5. TAB 3: Location Profile List & setup (PRO)
@Composable
fun ProfilesTab(viewModel: NetworkViewModel) {
    val isProUnlocked by viewModel.isProUnlocked.collectAsState()
    val locationProfiles by viewModel.locationProfiles.collectAsState()
    val context = LocalContext.current

    var profileName by remember { mutableStateOf("") }
    var userLat by remember { mutableStateOf("") }
    var userLon by remember { mutableStateOf("") }
    var preferredProfileMode by remember { mutableStateOf("5G Only") }

    val scope = rememberCoroutineScope()

    if (!isProUnlocked) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Geo Profiles requires PRO License", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Please navigate to the Optimizer panel to unlock premium options.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add Location Profile Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create Location profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    TextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name (e.g., Home)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF111317),
                            focusedContainerColor = Color(0xFF111317)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = userLat,
                            onValueChange = { userLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF111317),
                                focusedContainerColor = Color(0xFF111317)
                            )
                        )
                        TextField(
                            value = userLon,
                            onValueChange = { userLon = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF111317),
                                focusedContainerColor = Color(0xFF111317)
                            )
                        )
                    }

                    // Button to fetch current coordinates
                    Button(
                        onClick = {
                            val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fineLocation) {
                                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                try {
                                    fusedClient.lastLocation.addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            userLat = loc.latitude.toString()
                                            userLon = loc.longitude.toString()
                                            Toast.makeText(context, "Location Found!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Turn on GPS to fetch current location", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    Toast.makeText(context, "Permission Block", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Grant location permissions in Dashboard first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242936)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetch GPS Coordinates", color = Color.White)
                    }

                    // Radios or Row for preferredMode selection
                    Text("Auto Mode switch on Arrival:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            TelephonyHelper.MODE_5G_ONLY,
                            TelephonyHelper.MODE_4G_ONLY,
                            TelephonyHelper.MODE_5G_4G_BOTH,
                            TelephonyHelper.MODE_AUTO
                        ).forEach { modeLabel ->
                            val isChosen = preferredProfileMode == modeLabel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF111317))
                                    .clickable { preferredProfileMode = modeLabel }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = modeLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) Color(0xFF10B981) else Color.White
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val latVal = userLat.toDoubleOrNull()
                            val lonVal = userLon.toDoubleOrNull()
                            if (profileName.isNotBlank() && latVal != null && lonVal != null) {
                                viewModel.addLocationProfile(profileName, latVal, lonVal, preferredProfileMode)
                                profileName = ""
                                userLat = ""
                                userLon = ""
                                Toast.makeText(context, "Profile Added!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Complete all fields correctly", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_profile_btn")
                    ) {
                        Text("Save Geographical Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List Profile Title
        item {
            Text(
                "Active Profiles database",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (locationProfiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No location profiles saved.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }

        items(locationProfiles) { profile ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text(
                            "Coordinates: ${String.format("%.4f", profile.latitude)}, ${String.format("%.4f", profile.longitude)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF182A2E))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Applies: ${profile.preferredMode}",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.deleteLocationProfile(profile) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Profile",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

// 6. TAB 4: Help, Shizuku guides & verified devices
@Composable
fun HelpTab(viewModel: NetworkViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device validation database List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Verified Device Models (100% Works)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val devices = listOf(
                        "Google Pixel 系列" to "Unrestricted (Fully support NR-Only API lock)",
                        "OnePlus 系列" to "Supported (Bypass via secret RadioInfo setting)",
                        "Xiaomi/Redmi/POCO" to "Supported (Reflection API active or RadioInfo page)",
                        "Samsung Galaxy (Exynos/Snapdragon)" to "Partially blocked by Knox. Requires Shizuku or band selection tools."
                    )
                    
                    devices.forEach { (brand, state) ->
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(brand, fontWeight = FontWeight.SemiBold, color = Color.LightGray, fontSize = 13.sp)
                            Text(state, color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Shizuku setup
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Shizuku Power-User Method (No Root)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Allows power-users to grant secure ADB shell permissions direct to apps so they can edit global band preferences without a PC.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("1. Install Shizuku app in Play Store.", color = Color.LightGray, fontSize = 12.sp)
                    Text("2. Enable Wireless Debugging in Developer Options.", color = Color.LightGray, fontSize = 12.sp)
                    Text("3. Pair Shizuku and start service.", color = Color.LightGray, fontSize = 12.sp)
                    Text("4. Authorize '5G Only Network Mode' in Shizuku's app authorization console.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        // ADB Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "ADB Network Override Setup (With PC)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Connect device to computer using USB cable, enable Developer Options -> USB Debugging, and push command via terminal:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111317))
                            .padding(12.dp)
                    ) {
                        Text(
                            "adb shell settings put global preferred_network_mode 26",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Number 26 corresponds to NR_ONLY (5G Only), lock-in code 11 corresponds to LTE_ONLY (4G Only).",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Tutorial rewatch trigger
        item {
            Button(
                onClick = { viewModel.resetOnboarding() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Re-watch Tutorial Introduction", color = Color.White)
            }
        }
    }
}

// 7. Interactive Premium Sponsor / Ad Space Composable
@Composable
fun SponsoredAdCard() {
    var showSponsorDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sponsored_ad_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield Icon",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Secure 5G Proxy VPN",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                
                // SPONSOR badge label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SPONSOR",
                        color = Color(0xFF818CF8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Secure your high-speed mobile data. Prevent ISP throttling on 5G/LTE bands and encrypt stream traffic instantly with a 70% partner discount.",
                color = Color.LightGray,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { showSponsorDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ad_claim_details_button")
            ) {
                Text(
                    text = "Claim 70% Partner Discount",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showSponsorDialog) {
        AlertDialog(
            onDismissRequest = { showSponsorDialog = false },
            containerColor = Color(0xFF151821),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Premium Partner Offer",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "As a valued user of 5G SMART SWITCHER, you get exclusive partner access to secure multi-gigabit routing. Lock in your privacy today.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use Coupon Code at Checkout:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2230))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "5GONLY70",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSponsorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Copy & Close", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }
}
