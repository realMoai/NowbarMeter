package com.kakao.taxi.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import com.kakao.taxi.data.source.NetSpeedData
import com.kakao.taxi.data.source.impl.SpeedDataSource
import com.kakao.taxi.data.repository.DataStoreRepository
import java.util.Locale
import kotlin.math.roundToLong

class NetworkRepository(
    private val dataSource: SpeedDataSource,
    private val dataStoreRepository: DataStoreRepository,
) : KoinComponent {
    private val _isOverlayEnabled = MutableStateFlow(false)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _isLiveUpdateEnabled = MutableStateFlow(true)
    val isLiveUpdateEnabled: StateFlow<Boolean> = _isLiveUpdateEnabled.asStateFlow()

    private val _isNotificationEnabled = MutableStateFlow(true)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _isShowOnLockscreenEnabled = MutableStateFlow(false)
    val isShowOnLockscreenEnabled: StateFlow<Boolean> = _isShowOnLockscreenEnabled.asStateFlow()

    private val _isOverlayLocked = MutableStateFlow(false)
    val isOverlayLocked: StateFlow<Boolean> = _isOverlayLocked.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _netSpeed = MutableStateFlow(NetSpeedData(0, 0))
    val netSpeed: StateFlow<NetSpeedData> = _netSpeed.asStateFlow()

    private val _samplingInterval = MutableStateFlow(1500L)
    val samplingInterval: StateFlow<Long> = _samplingInterval.asStateFlow()

    private val _overlayBgColor = MutableStateFlow(0xCC000000.toInt())
    val overlayBgColor: StateFlow<Int> = _overlayBgColor.asStateFlow()

    private val _overlayTextColor = MutableStateFlow(0xFFFFFFFF.toInt())
    val overlayTextColor: StateFlow<Int> = _overlayTextColor.asStateFlow()

    private val _overlayCornerRadius = MutableStateFlow(8)
    val overlayCornerRadius: StateFlow<Int> = _overlayCornerRadius.asStateFlow()

    private val _overlayTextSize = MutableStateFlow(10f)
    val overlayTextSize: StateFlow<Float> = _overlayTextSize.asStateFlow()

    private val _overlayTextUp = MutableStateFlow("▲ ")
    val overlayTextUp: StateFlow<String> = _overlayTextUp.asStateFlow()

    private val _overlayTextDown = MutableStateFlow("▼ ")
    val overlayTextDown: StateFlow<String> = _overlayTextDown.asStateFlow()

    private val _overlayOrderUpFirst = MutableStateFlow(true)
    val overlayOrderUpFirst: StateFlow<Boolean> = _overlayOrderUpFirst.asStateFlow()

    private val _notificationTextUp = MutableStateFlow("▲ ")
    val notificationTextUp: StateFlow<String> = _notificationTextUp.asStateFlow()

    private val _notificationTextDown = MutableStateFlow("▼ ")
    val notificationTextDown: StateFlow<String> = _notificationTextDown.asStateFlow()

    private val _notificationOrderUpFirst = MutableStateFlow(true)
    val notificationOrderUpFirst: StateFlow<Boolean> = _notificationOrderUpFirst.asStateFlow()

    private val _notificationDisplayMode = MutableStateFlow(0)
    val notificationDisplayMode: StateFlow<Int> = _notificationDisplayMode.asStateFlow()

    private val _notificationTextSize = MutableStateFlow(0.60f)
    val notificationTextSize: StateFlow<Float> = _notificationTextSize.asStateFlow()

    private val _notificationUnitSize = MutableStateFlow(0.45f)
    val notificationUnitSize: StateFlow<Float> = _notificationUnitSize.asStateFlow()

    private val _notificationThreshold = MutableStateFlow(0L)
    val notificationThreshold: StateFlow<Long> = _notificationThreshold.asStateFlow()

    private val _notificationLowTrafficMode = MutableStateFlow(0)
    val notificationLowTrafficMode: StateFlow<Int> = _notificationLowTrafficMode.asStateFlow()

    private val _notificationUseCustomColor = MutableStateFlow(false)
    val notificationUseCustomColor: StateFlow<Boolean> = _notificationUseCustomColor.asStateFlow()

    private val _notificationColor = MutableStateFlow(0xFF888888.toInt())
    val notificationColor: StateFlow<Int> = _notificationColor.asStateFlow()

    private val _isHideFromRecents = MutableStateFlow(false)
    val isHideFromRecents: StateFlow<Boolean> = _isHideFromRecents.asStateFlow()

    private val _isOverlayUseDefaultColors = MutableStateFlow(false)
    val isOverlayUseDefaultColors: StateFlow<Boolean> = _isOverlayUseDefaultColors.asStateFlow()

    private val _isAutoStartServiceEnabled = MutableStateFlow(false)
    val isAutoStartServiceEnabled: StateFlow<Boolean> = _isAutoStartServiceEnabled.asStateFlow()

    private val _speedUnit = MutableStateFlow("0")
    val speedUnit: StateFlow<String> = _speedUnit.asStateFlow()

    private val _isOledThemeEnabled = MutableStateFlow(false)
    val isOledThemeEnabled: StateFlow<Boolean> = _isOledThemeEnabled.asStateFlow()

    private val _isCompactSpeedTextEnabled = MutableStateFlow(true)
    val isCompactSpeedTextEnabled: StateFlow<Boolean> = _isCompactSpeedTextEnabled.asStateFlow()

    private val _isBlankNotificationEnabled = MutableStateFlow(false)
    val isBlankNotificationEnabled: StateFlow<Boolean> = _isBlankNotificationEnabled.asStateFlow()

    private val _isNotificationTransparentIconEnabled = MutableStateFlow(false)
    val isNotificationTransparentIconEnabled: StateFlow<Boolean> = _isNotificationTransparentIconEnabled.asStateFlow()

    val skippedUpdateVersion: Flow<String> = dataStoreRepository.skippedUpdateVersion

    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var lastTotalRxBytes = 0L
    private var lastTotalTxBytes = 0L
    private var lastTime = 0L

    init {
        // 单次文件 IO 批量读取所有偏好设置，避免多次 first() 重复触发 DataStore 读取
        runBlocking {
            dataStoreRepository.allPreferences.first().let { prefs ->
                _isLiveUpdateEnabled.value = prefs[DataStoreRepository.KEY_LIVE_UPDATE] ?: true
                _isNotificationEnabled.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_ENABLED] ?: true
                _isShowOnLockscreenEnabled.value = prefs[DataStoreRepository.KEY_SHOW_ON_LOCKSCREEN] ?: false
                _isOverlayLocked.value = prefs[DataStoreRepository.KEY_OVERLAY_LOCKED] ?: false
                _isOverlayEnabled.value = prefs[DataStoreRepository.KEY_OVERLAY_ENABLED] ?: false
                _samplingInterval.value = prefs[DataStoreRepository.KEY_SAMPLING_INTERVAL] ?: 1500L
                _overlayBgColor.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_BG_COLOR] ?: 0xCC000000.toInt()
                _overlayTextColor.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_TEXT_COLOR] ?: 0xFFFFFFFF.toInt()
                _overlayCornerRadius.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_CORNER_RADIUS] ?: 8
                _overlayTextSize.value = prefs[DataStoreRepository.KEY_OVERLAY_TEXT_SIZE] ?: 10f
                _overlayTextUp.value = prefs[DataStoreRepository.KEY_OVERLAY_TEXT_UP] ?: "▲ "
                _overlayTextDown.value = prefs[DataStoreRepository.KEY_OVERLAY_TEXT_DOWN] ?: "▼ "
                _overlayOrderUpFirst.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_ORDER_UP_FIRST] ?: true
                _notificationTextUp.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_TEXT_UP] ?: "▲ "
                _notificationTextDown.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_TEXT_DOWN] ?: "▼ "
                _notificationOrderUpFirst.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_ORDER_UP_FIRST] ?: true
                _notificationDisplayMode.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_DISPLAY_MODE] ?: 0
                _notificationTextSize.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_TEXT_SIZE] ?: 0.60f
                _notificationUnitSize.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_UNIT_SIZE] ?: 0.45f
                _isHideFromRecents.value = prefs[DataStoreRepository.KEY_HIDE_FROM_RECENTS] ?: false
                _isOverlayUseDefaultColors.value =
                    prefs[DataStoreRepository.KEY_OVERLAY_USE_DEFAULT_COLORS] ?: false
                _isAutoStartServiceEnabled.value =
                    prefs[DataStoreRepository.KEY_AUTO_START_SERVICE] ?: false
                _notificationThreshold.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_THRESHOLD] ?: 0L
                _notificationLowTrafficMode.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_LOW_TRAFFIC_MODE] ?: 0
                _notificationUseCustomColor.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_USE_CUSTOM_COLOR] ?: false
                _notificationColor.value = prefs[DataStoreRepository.KEY_NOTIFICATION_COLOR] ?: 0xFF888888.toInt()
                _speedUnit.value = prefs[DataStoreRepository.KEY_SPEED_UNIT] ?: "0"
                _isOledThemeEnabled.value = prefs[DataStoreRepository.KEY_OLED_THEME] ?: false
                _isCompactSpeedTextEnabled.value =
                    prefs[DataStoreRepository.KEY_COMPACT_SPEED_TEXT] ?: true
                _isBlankNotificationEnabled.value =
                    prefs[DataStoreRepository.KEY_BLANK_NOTIFICATION] ?: false
                _isNotificationTransparentIconEnabled.value =
                    prefs[DataStoreRepository.KEY_NOTIFICATION_TRANSPARENT_ICON] ?: false
            }
        }
        scope.launch {
            dataStoreRepository.isLiveUpdateEnabled.collect { _isLiveUpdateEnabled.value = it }
        }
        scope.launch {
            dataStoreRepository.isNotificationEnabled.collect { _isNotificationEnabled.value = it }
        }
        scope.launch {
            dataStoreRepository.isShowOnLockscreenEnabled.collect { _isShowOnLockscreenEnabled.value = it }
        }
        scope.launch {
            dataStoreRepository.isOverlayLocked.collect { _isOverlayLocked.value = it }
        }
        scope.launch {
            dataStoreRepository.isOverlayEnabled.collect { _isOverlayEnabled.value = it }
        }
        scope.launch {
            dataStoreRepository.samplingInterval.collect { _samplingInterval.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayBgColor.collect { _overlayBgColor.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayTextColor.collect { _overlayTextColor.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayCornerRadius.collect { _overlayCornerRadius.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayTextSize.collect { _overlayTextSize.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayTextUp.collect { _overlayTextUp.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayTextDown.collect { _overlayTextDown.value = it }
        }
        scope.launch {
            dataStoreRepository.overlayOrderUpFirst.collect { _overlayOrderUpFirst.value = it }
        }
        scope.launch {
            dataStoreRepository.notificationTextUp.collect { _notificationTextUp.value = it }
        }
        scope.launch {
            dataStoreRepository.notificationTextDown.collect { _notificationTextDown.value = it }
        }
        scope.launch {
            dataStoreRepository.notificationOrderUpFirst.collect {
                _notificationOrderUpFirst.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationDisplayMode.collect {
                _notificationDisplayMode.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationTextSize.collect {
                _notificationTextSize.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationUnitSize.collect {
                _notificationUnitSize.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isHideFromRecents.collect {
                _isHideFromRecents.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isOverlayUseDefaultColors.collect {
                _isOverlayUseDefaultColors.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isAutoStartServiceEnabled.collect {
                _isAutoStartServiceEnabled.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationThreshold.collect {
                _notificationThreshold.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationLowTrafficMode.collect {
                _notificationLowTrafficMode.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationUseCustomColor.collect {
                _notificationUseCustomColor.value = it
            }
        }
        scope.launch {
            dataStoreRepository.notificationColor.collect {
                _notificationColor.value = it
            }
        }
        scope.launch {
            dataStoreRepository.speedUnit.collect {
                _speedUnit.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isOledThemeEnabled.collect {
                _isOledThemeEnabled.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isCompactSpeedTextEnabled.collect {
                _isCompactSpeedTextEnabled.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isBlankNotificationEnabled.collect {
                _isBlankNotificationEnabled.value = it
            }
        }
        scope.launch {
            dataStoreRepository.isNotificationTransparentIconEnabled.collect {
                _isNotificationTransparentIconEnabled.value = it
            }
        }
    }

    fun setOverlayEnabled(enable: Boolean) {
        scope.launch { dataStoreRepository.setOverlayEnabled(enable) }
    }

    fun setLiveUpdateEnabled(enable: Boolean) {
        scope.launch { dataStoreRepository.setLiveUpdateEnabled(enable) }
    }

    fun setNotificationEnabled(enable: Boolean) {
        scope.launch { dataStoreRepository.setNotificationEnabled(enable) }
    }

    fun setShowOnLockscreenEnabled(enable: Boolean) {
        scope.launch { dataStoreRepository.setShowOnLockscreenEnabled(enable) }
    }

    fun setOverlayLocked(locked: Boolean) {
        scope.launch { dataStoreRepository.setOverlayLocked(locked) }
    }

    fun setSamplingInterval(interval: Long) {
        scope.launch { dataStoreRepository.setSamplingInterval(interval) }
    }

    fun setOverlayBgColor(color: Int) {
        scope.launch { dataStoreRepository.setOverlayBgColor(color) }
    }

    fun setOverlayTextColor(color: Int) {
        scope.launch { dataStoreRepository.setOverlayTextColor(color) }
    }

    fun setOverlayCornerRadius(radius: Int) {
        scope.launch { dataStoreRepository.setOverlayCornerRadius(radius) }
    }

    fun setOverlayTextSize(size: Float) {
        scope.launch { dataStoreRepository.setOverlayTextSize(size) }
    }

    fun setOverlayTextUp(text: String) {
        scope.launch { dataStoreRepository.setOverlayTextUp(text) }
    }

    fun setOverlayTextDown(text: String) {
        scope.launch { dataStoreRepository.setOverlayTextDown(text) }
    }

    fun setOverlayOrderUpFirst(upFirst: Boolean) {
        scope.launch { dataStoreRepository.setOverlayOrderUpFirst(upFirst) }
    }

    fun setNotificationTextUp(text: String) {
        scope.launch { dataStoreRepository.setNotificationTextUp(text) }
    }

    fun setNotificationTextDown(text: String) {
        scope.launch { dataStoreRepository.setNotificationTextDown(text) }
    }

    fun setNotificationOrderUpFirst(upFirst: Boolean) {
        scope.launch { dataStoreRepository.setNotificationOrderUpFirst(upFirst) }
    }

    fun setNotificationDisplayMode(mode: Int) {
        scope.launch { dataStoreRepository.setNotificationDisplayMode(mode) }
    }

    fun setNotificationTextSize(size: Float) {
        scope.launch { dataStoreRepository.setNotificationTextSize(size) }
    }

    fun setNotificationUnitSize(size: Float) {
        scope.launch { dataStoreRepository.setNotificationUnitSize(size) }
    }

    fun setHideFromRecents(hide: Boolean) {
        scope.launch { dataStoreRepository.setHideFromRecents(hide) }
    }

    fun setOverlayUseDefaultColors(useDefault: Boolean) {
        scope.launch { dataStoreRepository.setOverlayUseDefaultColors(useDefault) }
    }

    fun setAutoStartServiceEnabled(enabled: Boolean) {
        scope.launch { dataStoreRepository.setAutoStartServiceEnabled(enabled) }
    }

    fun setNotificationThreshold(threshold: Long) {
        scope.launch { dataStoreRepository.setNotificationThreshold(threshold) }
    }

    fun setNotificationLowTrafficMode(mode: Int) {
        scope.launch { dataStoreRepository.setNotificationLowTrafficMode(mode) }
    }

    fun setNotificationUseCustomColor(useCustom: Boolean) {
        scope.launch { dataStoreRepository.setNotificationUseCustomColor(useCustom) }
    }

    fun setNotificationColor(color: Int) {
        scope.launch { dataStoreRepository.setNotificationColor(color) }
    }

    fun setSpeedUnit(unit: String) {
        scope.launch { dataStoreRepository.setSpeedUnit(unit) }
    }

    fun setOledThemeEnabled(enabled: Boolean) {
        scope.launch { dataStoreRepository.setOledThemeEnabled(enabled) }
    }

    fun setCompactSpeedTextEnabled(enabled: Boolean) {
        scope.launch { dataStoreRepository.setCompactSpeedTextEnabled(enabled) }
    }

    fun setBlankNotificationEnabled(enabled: Boolean) {
        scope.launch { dataStoreRepository.setBlankNotificationEnabled(enabled) }
    }

    fun setNotificationTransparentIconEnabled(enabled: Boolean) {
        scope.launch { dataStoreRepository.setNotificationTransparentIconEnabled(enabled) }
    }

    suspend fun setSkippedUpdateVersion(version: String) {
        dataStoreRepository.setSkippedUpdateVersion(version)
    }

    suspend fun getOverlayPosition(): Pair<Int, Int> {
        val x = dataStoreRepository.overlayX.first()
        val y = dataStoreRepository.overlayY.first()
        return x to y
    }

    fun saveOverlayPosition(x: Int, y: Int) {
        scope.launch {
            dataStoreRepository.saveOverlayPosition(x, y)
        }
    }

    fun startMonitoring() {
        if (_isMonitoring.value) return
        Log.i(TAG, "request start monitoring")
        _isMonitoring.value = true

        monitoringJob = scope.launch {
            val initialData = dataSource.getTrafficData()
            lastTotalRxBytes = initialData.rxBytes
            lastTotalTxBytes = initialData.txBytes
            lastTime = System.currentTimeMillis()

            while (isActive) {
                val startTime = System.currentTimeMillis()
                val interval = _samplingInterval.value

                withContext(Dispatchers.IO) {
                    val trafficData = dataSource.getTrafficData()
                    val totalRxBytes = trafficData.rxBytes
                    val totalTxBytes = trafficData.txBytes
                    val currentTime = System.currentTimeMillis()

                    val rxDelta = totalRxBytes - lastTotalRxBytes
                    val txDelta = totalTxBytes - lastTotalTxBytes
                    val timeDelta = currentTime - lastTime

                    if (timeDelta > 0) {
                        // Calculate speed
                        val downloadSpeed = ((rxDelta * 1000) / timeDelta).coerceAtLeast(0)
                        val uploadSpeed = ((txDelta * 1000) / timeDelta).coerceAtLeast(0)

                        _netSpeed.value = NetSpeedData(
                            downloadSpeed.coerceAtLeast(0),
                            uploadSpeed.coerceAtLeast(0)
                        )
                    }

                    lastTotalRxBytes = totalRxBytes
                    lastTotalTxBytes = totalTxBytes
                    lastTime = currentTime
                }

                // Delay to achieve the desired interval
                val delayMills = interval - (System.currentTimeMillis() - startTime)
                delay(delayMills.coerceAtLeast(0))
            }
        }
    }

    fun stopMonitoring() {
        Log.i(TAG, "request stop monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
        _isMonitoring.value = false
        _netSpeed.value = NetSpeedData(0, 0)
    }

    companion object {
        private const val TAG = "NetworkRepository"

        /**
         * 根据数值大小格式化小数位数：>= 100 → 0 位，>= 10 → 1 位，否则 2 位
         */
        private fun formatFixedValue(value: Double, compact: Boolean = false): String {
            if (compact) {
                return when {
                    value >= 10.0 -> value.roundToLong().toString()
                    else -> "%.1f".format(Locale.getDefault(), value)
                }
            }
            val pattern = when {
                value >= 100 -> "%.0f"
                value >= 10 -> "%.1f"
                else -> "%.2f"
            }
            return pattern.format(Locale.getDefault(), value)
        }

        fun formatSpeedText(
            bytes: Long,
            speedUnit: String = "0",
            compact: Boolean = false
        ): Pair<String, String> {
            val selectedUnits = speedUnit.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            val useAuto = selectedUnits.isEmpty() || selectedUnits.contains(0)

            val unitToUse = if (useAuto) {
                when {
                    bytes >= 1073741824.0 -> 4
                    bytes >= 1048576.0 -> 3
                    bytes >= 1024.0 -> 2
                    else -> 1
                }
            } else {
                // Scaling logic constrained to selected units
                val sortedAvailable = selectedUnits.sortedDescending()
                var best = sortedAvailable.last()
                for (unit in sortedAvailable) {
                    val threshold = when (unit) {
                        4 -> 1024 * 1024 * 1024L
                        3 -> 1024 * 1024L
                        2 -> 1024L
                        else -> 0L
                    }
                    if (bytes >= threshold) {
                        best = unit
                        break
                    }
                }
                best
            }

            return when (unitToUse) {
                1 -> bytes.toString() to "B/s"
                2 -> formatFixedValue(bytes / 1024.0, compact) to "KB/s"
                3 -> {
                    val mb = bytes / 1048576.0
                    val formatted = if (compact) {
                        if (mb >= 10.0) mb.roundToLong().toString()
                        else "%.1f".format(Locale.getDefault(), mb)
                    } else {
                        if (mb < 10) "%.1f".format(Locale.getDefault(), mb)
                        else "%.0f".format(Locale.getDefault(), mb)
                    }
                    formatted to "MB/s"
                }
                4 -> {
                    val gb = bytes / 1073741824.0
                    val formatted = if (compact) {
                        if (gb >= 10.0) gb.roundToLong().toString()
                        else "%.1f".format(Locale.getDefault(), gb)
                    } else {
                        "%.1f".format(Locale.getDefault(), gb)
                    }
                    formatted to "GB/s"
                }
                else -> bytes.toString() to "B/s"
            }
        }

        fun formatSpeedLine(
            bytes: Long,
            speedUnit: String = "0",
            compact: Boolean = false
        ): String {
            val (v, u) = formatSpeedText(bytes, speedUnit, compact)
            return "$v$u"
        }

        fun formatSpeedTextForLiveUpdate(
            bytes: Long,
            speedUnit: String = "0",
            compact: Boolean = false
        ): String {
            val (v, u) = formatSpeedText(bytes, speedUnit, compact)
            return "$v$u"
        }
    }
}
