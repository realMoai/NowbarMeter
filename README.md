# Nowbar Meter

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="200" alt="Nowbar Meter Logo"/>
</p>

<p align="center">
  <strong>Precise internet speed monitor hijacked directly into the Samsung One UI Now Bar.</strong>
</p>

<p align="center">
    <a href="LICENSE"><img src="https://img.shields.io/github/license/realMoai/NowbarMeter" alt="License"></a>
    <img src="https://img.shields.io/badge/Version-1.0-green" alt="Version 1.0">
    <img src="https://img.shields.io/badge/Forked_From-Pixel_Meter-blue" alt="Forked from Pixel Meter">
</p>

## About

NowbarMeter is a specialized fork of [Pixel Meter](https://github.com/Mystery00/PixelMeter), rebuilt specifically for Samsung Galaxy devices.

Samsung severely restricted the **Now Bar** (Live Updates / Status Bar Chip) feature in modern One UI builds, locking it behind a hardcoded system whitelist of proprietary and partner apps. **NowbarMeter bypasses this restriction.** By injecting the `com.samsung.android.support.ongoing_activity` metadata flag and strategically spoofing a whitelisted package name (e.g., `com.kakao.taxi`), this app forces a native, real-time network speed monitor directly into the One UI Now Bar pill.

It retains all the god-tier features of the original app, including intelligent VPN traffic filtering, so your displayed speed is exactly what your physical interface is actually pulling.

## Features

- **Samsung Now Bar Native Integration**: Hijacks the One UI status bar chip to display real-time upload and download speeds perfectly native to the system UI.
- **Whitelist Bypass**: Uses package spoofing to bypass Samsung's walled-garden restrictions.
- **Customizable Speed Units**: Select multiple units (B/s, KB/s, MB/s, GB/s) via checkboxes. The app automatically scales within your selected set.
- **OLED Theme**: Pure black theme support for OLED screens, perfect for Galaxy devices.
- **Precise Traffic Stats**: Uses `ConnectivityManager` and `TrafficStats` to filter out `tun0` and virtual VPN interfaces to prevent double-counting data.
- **Modern UI**: Built entirely with Jetpack Compose and Material 3.
- **Privacy Focused**: 100% local processing. No network data is ever logged or uploaded.

## How It Works (The Hack)

To get around Samsung's locked UI, this fork executes two specific workarounds:
1. **Manifest Injection**: Adds the `ongoing_activity` metadata tag required by One UI's system interceptor to recognize a foreground service as a Live Notification.
2. **Package Spoofing**: Modifies the `applicationId` to perfectly match a pre-approved app from Samsung's internal whitelist.

### ⚠️ Important Installation Note
Because this app intentionally spoofs another application's package name and signature, **Google Play Protect will flag it.** When installing the APK, Play Protect will warn you that the app did not come from the Play Store and has a different signature. This is the cryptographic security working exactly as intended. **Do not update this app via the Play Store**, or your speed meter will be overwritten by a Korean ride-hailing app.

## Requirements

- **Device**: Samsung Galaxy devices running One UI (Requires a version supporting the Now Bar/Live Updates feature).
- **Android Version**: Android 12 (API Level 31) or higher.
- **Permissions**: Notification (Crucial for the foreground service to push to the Now Bar).

## Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture Pattern**: MVVM + Clean Architecture
- **Dependency Injection**: Koin
- **Data Source**: `TrafficStats` + `ConnectivityManager`

## Credits

Massive shoutout to [Mystery00](https://github.com/Mystery00) for building the original, open-source [Pixel Meter](https://github.com/Mystery00/PixelMeter). This fork simply adapts their brilliant networking logic to break through Samsung's UI restrictions.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
