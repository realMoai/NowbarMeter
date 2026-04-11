package com.kakao.taxi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.taxi.data.repository.NetworkRepository
import com.kakao.taxi.data.source.NetSpeedData
import com.kakao.taxi.ui.MainViewModel
import com.kakao.taxi.ui.settings.SettingsActivity
import com.kakao.taxi.ui.theme.PixelPulseTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val isOledTheme by viewModel.isOledThemeEnabled.collectAsState(initial = false)
            PixelPulseTheme(isOledTheme = isOledTheme) {
                HomeScreen()
            }
        }
    }

    private fun launchSpeedTest(context: android.content.Context) {
        val url = "https://speed.cloudflare.com"
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, url.toUri())
    }

    @Composable
    fun HomeScreen() {
        val context = LocalContext.current
        val view = LocalView.current
        val speed by viewModel.currentSpeed.collectAsState()
        val isServiceRunning by viewModel.isServiceRunning.collectAsState()
        val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()
        val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
        val isHideFromRecents by viewModel.isHideFromRecents.collectAsState(initial = false)
        val serviceError by viewModel.serviceStartError.collectAsState()
        val speedUnit by viewModel.speedUnit.collectAsState()
        val compactMode by viewModel.isCompactSpeedTextEnabled.collectAsState(initial = false)
        val isOledTheme by viewModel.isOledThemeEnabled.collectAsState(initial = false)

        val updateInfo by viewModel.updateAvailable.collectAsState()
        val skippedVersion by viewModel.skippedUpdateVersion.collectAsState(initial = "")

        LaunchedEffect(skippedVersion) {
            viewModel.checkForUpdates(BuildConfig.VERSION_NAME, skippedVersion)
        }

        if (updateInfo != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearUpdateDialog() },
                title = { Text("Update Available") },
                text = { Text("Version ${updateInfo!!.versionName} is now available on GitHub.") },
                confirmButton = {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, updateInfo!!.url.toUri())
                        context.startActivity(intent)
                        viewModel.clearUpdateDialog()
                    }) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.skipUpdate(updateInfo!!.versionName)
                    }) {
                        Text("Skip")
                    }
                }
            )
        }

        val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
        var isIgnoringBattery by remember {
            mutableStateOf<Boolean>(powerManager.isIgnoringBatteryOptimizations(context.packageName))
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(isHideFromRecents) {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val tasks = activityManager.appTasks
            if (tasks.isNotEmpty()) {
                tasks[0].setExcludeFromRecents(isHideFromRecents)
            }
        }

        // Permission Launcher
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    viewModel.clearError()
                }
            }
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SpeedDashboardCard(speed, speedUnit, compactMode)
                }

                // Configuration Section
                if (serviceError != null || !isIgnoringBattery) {
                    item {
                        Text(
                            stringResource(R.string.title_configuration),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Service Permission Error Card
                if (serviceError != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    serviceError?.first ?: stringResource(R.string.error_unknown),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Spacer(modifier = Modifier.weight(1F))
                                    Button(onClick = {
                                        serviceError?.let { (_, action) ->
                                            if (action == Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    val intent = Intent(action)
                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    intent.putExtra(
                                                        Settings.EXTRA_APP_PACKAGE,
                                                        context.packageName
                                                    )
                                                    context.startActivity(intent)
                                                }
                                            } else {
                                                val intent = Intent(action)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                intent.data =
                                                    "package:${context.packageName}".toUri()
                                                context.startActivity(intent)
                                                viewModel.clearError()
                                            }
                                        }
                                    }) {
                                        Text(stringResource(R.string.action_request_fix))
                                    }
                                    TextButton(
                                        onClick = { viewModel.clearError() },
                                    ) {
                                        Text(stringResource(R.string.action_dismiss))
                                    }
                                }
                            }
                        }
                    }
                }

                // Battery Optimization Warning Card
                if (!isIgnoringBattery) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.battery_optimization_warning_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    stringResource(R.string.battery_optimization_warning_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.title_monitor_control),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isServiceRunning) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isServiceRunning) stringResource(R.string.status_monitor_running) else stringResource(
                                        R.string.status_monitor_stopped
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Start Button
                                Button(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        viewModel.startService()
                                    },
                                    enabled = !isServiceRunning,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.action_start))
                                }
                                // Stop Button
                                Button(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        viewModel.stopService()
                                    },
                                    enabled = isServiceRunning,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.action_stop))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.title_feature_config),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    ConfigRow(
                        title = stringResource(R.string.config_enable_overlay),
                        subtitle = stringResource(R.string.config_enable_overlay_desc),
                        checked = isOverlayEnabled,
                        onCheckedChange = { viewModel.setOverlayEnabled(it) }
                    )
                }

                item {
                    ConfigRow(
                        title = stringResource(R.string.config_enable_notification),
                        subtitle = stringResource(R.string.config_enable_notification_desc),
                        checked = isNotificationEnabled,
                        onCheckedChange = { viewModel.setNotificationEnabled(it) }
                    )
                }

                item {
                    Text(
                        stringResource(R.string.title_tools),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            launchSpeedTest(context)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOledTheme && isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = if (isOledTheme && isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.action_speed_test),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isOledTheme && isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.desc_speed_test),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isOledTheme && isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedDashboardCard(speed: NetSpeedData, speedUnit: String = "0", compactMode: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.label_total_speed),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                NetworkRepository.formatSpeedLine(speed.totalSpeed, speedUnit, compactMode),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.label_download),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "▼ " + NetworkRepository.formatSpeedLine(speed.downloadSpeed, speedUnit, compactMode),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.label_upload),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "▲ " + NetworkRepository.formatSpeedLine(speed.uploadSpeed, speedUnit, compactMode),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { isChecked ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    view.performHapticFeedback(if (isChecked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                onCheckedChange(isChecked)
            },
            enabled = enabled
        )
    }
}
