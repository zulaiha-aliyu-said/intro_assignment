# CapCutMini

Free, offline-first Android video editor (Kotlin + Jetpack Compose + Media3 + Room).

## Current milestones

### Milestone 1
- Dark-themed Compose UI, four screens, navigation
- SAF video picker + ExoPlayer preview
- Room project metadata

### Fades / Transitions
- Video + audio fades (0.5–3s) in preview and Transformer export
- Fade-through-black transitions (Cross\* = same, Media3 1.5.1 limitation)
- Trim, manual subtitles, MP4 export
- Effects settings persisted in Room `editJson`
- Details: `docs/MILESTONE_FADES.md`

## Run on device/emulator (Android Studio — recommended)

1. Install [Android Studio](https://developer.android.com/studio) (JDK 17 + Android SDK).
2. Open this folder: `File > Open` → select `Video editor`.
3. Wait for Gradle sync.
4. Start an emulator **or** connect a phone with USB debugging.
5. Click **Run** ▶ (app `com.capcutmini.app`).

## Build a debug APK (CLI)

From this project folder in PowerShell:

```powershell
# Optional if java is not on PATH:
# $env:JAVA_HOME = "C:\Path\To\jdk-17"

.\gradlew.bat :app:assembleDebug
```

APK path:

`app\build\outputs\apk\debug\app-debug.apk`

Install:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Or install + launch via Gradle:

```powershell
.\gradlew.bat :app:installDebug
```

`local.properties` must contain your SDK path (Android Studio creates it). Example:

```
sdk.dir=C\:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
```

See `docs/DEPENDENCIES.md` for every library and why it is included.
