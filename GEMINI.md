## Code Quality and Development Principles

### Quality Requirements

- **Language Standards**: Strictly follow the official Kotlin coding conventions.
- **Android Standards**:
  - Follow the Modern Android Development (MAD) guidelines.
  - **UI Framework**: Use Jetpack Compose exclusively (Material 3/Material You/Material Express), supporting dynamic color extraction on supported devices.
  - **Architecture**: MVVM (ViewModel + StateFlow + Repository).
  - **Dependency Injection**: Use **Koin** (Koin‑Android, Koin‑Compose).
  - **Compatibility**: MinSDK = 31 (Android 12), TargetSDK = latest (36).

- **Feature Specification**:
  - **Single Data Source**:
    - Use `TrafficStats` together with `ConnectivityManager`.
    - **Core Logic**: Iterate physical network interfaces (Wi‑Fi, Cellular, Ethernet) via `ConnectivityManager`, exclude VPN virtual interfaces (to avoid double‑counting traffic), and read `TrafficStats` directly.
    - **Strictly Prohibited**: Using root or Shizuku permissions for unnecessary low‑level operations.
  - **UI Presentation**:
    - No display on first launch; the user decides whether to enable it.
    - Notification bar dynamic icon: draw a bitmap in real time; in bidirectional mode, merge and show total traffic.
    - Floating Window: mount a Compose view to `WindowManager`, with an independent toggle.
    - **Quick Settings**: provide a `TileService` for a shortcut toggle in the system pull‑down panel.

- **Code Structure**:
  - A single file should generally not exceed 1,000 lines.
  - **Service Separation**: The core speed‑monitoring logic must run in a foreground service (`type = dataSync`).
  - Avoid writing business logic inside Activities.

- **Comments**:
  - Write clear KDoc and inline comments in English.
  - Core algorithm logic (e.g., network‑interface filtering) must have detailed explanatory comments.

### Testing and Verification

- **Compile Check**: After any code change, run `./gradlew :app:assembleDebug` to ensure the project builds.
- **Lint Check**: Execute `./gradlew lint` to detect potential quality issues.
- **Functional Verification**:
  - Specifically test that speed statistics are accurate when a VPN is active (VPN virtual‑adapter traffic must not be counted).
  - Tests must be performed on real devices, preferably Samsung Galaxy phones.

#### Local Project Validation Process

1. Run `./gradlew :app:assembleDebug` from the command line.
2. Confirm there are no compilation errors and that `libs.versions.toml` shows no outdated warnings.

## Documentation and Memory

Documentation is stored in Markdown files under `.agentdocs/` and its sub‑directories.  
Index document: `.agentdocs/index.md`

### Documentation Categories

- `prd/` – Product and requirements
  - `prd/requirements.md` – Core feature requirements (Nowbar Meter feature list)
- `architecture/` – Architecture and technical details
  - `architecture/data-source-strategy.md` – Hybrid data‑source strategy (Shizuku vs. standard API)
  - `architecture/service-lifecycle.md` – Foreground‑service keep‑alive and Android 14+ adaptation
- `ui/` – UI specifications
  - `ui/design-system.md` – Material 3 theme and floating‑window design guidelines
- `workflow/` – Task‑flow documents (named according to the standard format)

### Global Important Memory

- **Project Name**: Nowbar Meter
- **Device Support**:
  - Core target: Samsung Galaxy series running OneUI 7 and up.
  - Compatibility target: Devices running native or near‑native Android (AOSP).
- **SDK Versions**:
  - **MinSDK**: 31 (Android 12)
  - **CompileSDK / TargetSDK**: 36 (Android 16)
- **Key Technical Decisions**:
  - **DI**: Koin (lightweight, suitable for this tool).
  - **Settings UI**: `me.zhanghai.compose.preference` + `com.github.skydoves:colorpicker-compose`.
  - **Browser**: Chrome Custom Tabs (CCT) for integrating Cloudflare speed tests.

## Task Handling Guide

- **Requirement Clarification**: When a requirement is vague (e.g., exact filter‑interface name, floating‑window anchoring logic), ask clarifying questions first.
- **Solution Analysis**: For any user‑proposed idea, analyze it against Android system limits (especially background restrictions).
- **Phased Implementation**:
  1. Build the basic architecture (Koin, Compose, Navigation).
  2. Implement speed monitoring (TrafficStats + ConnectivityManager).
  3. Develop the UI layer (notification icon drawing, floating window).
  4. Integrate CCT and complete the settings page.
- **Risk Recording**: Any work that touches low‑level system parts (e.g., reading `/proc`) or modifies keep‑alive strategies must be documented with potential compatibility risks.

### Task Review

Before delivering the final message for a completed task, perform the following review:
- Check whether new reusable components were created and update the architecture docs accordingly.
- Verify whether any documents under `.agentdocs/` need updating.
- Ensure dependency versions in `libs.versions.toml` are the latest stable releases.

## Communication Principles

- All replies, documentation, and code comments to the user must be written in English.
- When professional terms appear (e.g., “Interface”, “Tun”, “Binder”, “Overlay”), keep the English term and, if needed, provide a brief explanation.  
