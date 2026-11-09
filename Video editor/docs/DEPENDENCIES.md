# CapCutMini dependencies (Milestone 1)

## Build plugins

| Dependency | Purpose |
|---|---|
| `com.android.application` 8.7.3 | Android Gradle Plugin — builds the APK |
| `org.jetbrains.kotlin.android` 2.0.21 | Kotlin compiler for Android |
| `org.jetbrains.kotlin.plugin.compose` 2.0.21 | Compose compiler plugin (Kotlin 2.0+) |
| `com.google.devtools.ksp` 2.0.21-1.0.28 | Kotlin Symbol Processing for Room code generation |

## Runtime libraries

| Dependency | Purpose |
|---|---|
| `androidx.compose:compose-bom` | Aligns Compose library versions |
| `androidx.compose.ui:ui` | Core Compose UI |
| `androidx.compose.material3:material3` | Material 3 dark theme components |
| `androidx.compose.material:material-icons-extended` | Navigation icons |
| `androidx.activity:activity-compose` | `ComponentActivity` + Compose entry |
| `androidx.lifecycle:lifecycle-*` | ViewModel + lifecycle-aware Flow collection |
| `androidx.navigation:navigation-compose` | In-app screen navigation |
| `androidx.media3:media3-exoplayer` | Offline local video playback |
| `androidx.media3:media3-ui` | `PlayerView` controller UI |
| `androidx.media3:media3-transformer` | Reserved for real edit/export later (not used yet) |
| `androidx.media3:media3-effect` | Companion to Transformer for later milestones |
| `androidx.media3:media3-common` | Shared Media3 types (`MediaItem`, etc.) |
| `androidx.room:room-runtime` / `room-ktx` | Offline project database |
| `androidx.room:room-compiler` (KSP) | Generates Room DAO implementations |
| `kotlinx-coroutines-android` | Async work on the main/UI dispatcher |

No Firebase, billing, paid SDKs, or embedded API secrets.
