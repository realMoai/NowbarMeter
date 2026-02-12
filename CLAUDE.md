# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## 项目概述

PixelMeter 是一款面向 Google Pixel 及原生 Android 设备的网速监控应用。核心特性：通过
`ConnectivityManager` + `TrafficStats` 过滤 VPN 虚拟接口（如
tun0），仅统计物理网络接口（Wi-Fi、Cellular、Ethernet）的流量，避免 VPN 场景下流量双重计数。

## 构建命令

```bash
# Debug 构建
./gradlew :app:assembleDebug

# Release 构建（需要在 local.properties 中配置签名信息）
./gradlew :app:assembleRelease

# Lint 检查
./gradlew lint
```

项目当前没有单元测试或 Android 测试。

## SDK 与工具链

- **MinSDK**: 31 (Android 12)，**CompileSDK/TargetSDK**: 36 (Android 16)
- **Kotlin**: 2.3.0，**JVM Target**: 21，**AGP**: 8.13.2
- **Compose BOM**: 2026.01.00
- 全局 opt-in：`ExperimentalMaterial3Api`
- 版本目录：`gradle/libs.versions.toml`
- 版本号策略：`versionCode` 来自 git commit 数量，`versionName` 来自 `libs.versions.toml` 中的
  `app-version`
- 支持语言：`en`、`zh-rCN`

## 架构

单模块应用（`app/`），采用 **MVVM** 架构，使用 Kotlin + Jetpack Compose (Material 3)，依赖注入使用 **Koin
**。

包根路径：`vip.mystery0.pixel.meter`，位于 `app/src/main/kotlin/`。

### 数据流

```
SpeedDataSource (ISpeedDataSource)
  ↓ 通过 TrafficStats 逐接口读取物理网卡流量
NetworkRepository
  ↓ 定时轮询数据源，计算速率差值 → StateFlow<NetSpeedData>
  ↓ 从 DataStoreRepository 同步所有用户偏好设置为 StateFlow
NetworkMonitorService (前台服务)
  ↓ 收集 netSpeed flow
  ├→ NotificationHelper：将网速渲染为动态 Bitmap 图标或 Live Update 文本
  └→ OverlayWindow：基于 Compose 的悬浮窗，通过 WindowManager 挂载
```

### 核心组件

- **`SpeedDataSource`** (`data/source/impl/`) — 注册 `NetworkCallback`，过滤 Wi-Fi/Cellular/Ethernet
  物理接口，用 `ConcurrentHashMap` 缓存接口名，调用 `TrafficStats.getRxBytes/getTxBytes` 逐接口读取流量。这是
  VPN 流量过滤的核心。
- **`NetworkRepository`** (`data/repository/`) — 中央状态枢纽。轮询数据源计算速率差值，暴露约 25 个
  `StateFlow` 属性（镜像自 `DataStoreRepository`）。所有设置写入委托给 `DataStoreRepository`。
- **`DataStoreRepository`** (`data/repository/`) — Jetpack DataStore Preferences 封装层。DataStore
  名称：`pixel_pulse_preferences`。
- **`NetworkMonitorService`** (`service/`) — 前台服务（类型 `specialUse|dataSync`）。收集网速
  flow，更新通知和悬浮窗。监听息屏/亮屏事件，息屏 2 分钟后暂停监控以省电。
- **`NotificationHelper`** (`service/`) — 构建通知，支持实时 Bitmap 图标（Canvas 绘制网速文字）或
  Android 16+ Live Update（`setShortCriticalText`）。
- **`OverlayWindow`** (`ui/overlay/`) — 基于 Compose 的悬浮窗，挂载到 `WindowManager`。实现
  `LifecycleOwner`/`ViewModelStoreOwner`/`SavedStateRegistryOwner` 以在 Activity 外宿主 Compose。
- **`AppModule`** (`di/`) — 单一 Koin 模块，注册所有依赖。
- **Tile Services** (`service/tile/`) — Quick Settings 快捷磁贴，用于开关通知和悬浮窗。
- **`BootReceiver`** (`receiver/`) — 开机自启服务（用户启用时生效）。

### Activities

- `MainActivity` — 主界面仪表盘
- `SettingsActivity` — 设置页面，使用 `me.zhanghai.compose.preference` 库

## 开发规范（来自 GEMINI.md）

- **注释与文档使用中文**，专业术语（Interface、Tun、Overlay 等）保留英文。
- 遵循 Kotlin 官方编码规范和 Modern Android Development (MAD) 指南。
- 核心功能严格禁止使用 Root 或 Shizuku 权限。
- 单个文件原则上不超过 1000 行。
- 业务逻辑放在前台服务中，不在 Activity 中编写。

## 签名配置

Release 签名从 `local.properties` 读取（键名：`SIGN_KEY_STORE_FILE`、`SIGN_KEY_STORE_PASSWORD`、
`SIGN_KEY_ALIAS`、`SIGN_KEY_PASSWORD`），如果文件不可读则回退到同名环境变量。
