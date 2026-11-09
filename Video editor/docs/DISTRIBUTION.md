# CapCutMini APK distribution

## Inspected app identity (do not change casually)

| Field | Value |
|--------|--------|
| `applicationId` | `com.capcutmini.app` |
| `namespace` | `com.capcutmini.app` |
| Current `versionName` | `0.3.0` |
| Current `versionCode` | `1` |
| `minSdk` | 26 |
| `targetSdk` / `compileSdk` | 35 |

Updates require the **same** `applicationId` and the **same** release signing certificate.

## Release signing (local secrets)

1. Create a keystore once:

```powershell
.\scripts\create-release-keystore.ps1
```

This writes (both **gitignored**):

- `signing/capcutmini-release.jks`
- `keystore.properties`

2. Or copy `keystore.properties.example` → `keystore.properties` and point `storeFile` at your existing `.jks`.

3. **Back up** `signing/` and `keystore.properties` offline. Losing them blocks seamless updates.

4. Never commit passwords, keystores, or `keystore.properties`.

## Build release APK

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-17"   # if needed
.\gradlew.bat :app:assembleRelease
```

Signed output (when `keystore.properties` is present):

`app\build\outputs\apk\release\app-release.apk`

If signing config is missing, Gradle may still produce an unsigned or debug-signed artifact — do **not** distribute that for updates.

Suggested public name: `CapCutMini-0.3.0.apk`.

## Website + version manifest

- Site root: `website/`
- Manifest: `website/version.json` (version, notes, `downloadUrl`)
- Cloudflare Pages: deploy `website` as the Pages root (see `website/README.md`)
- After each GitHub Release, set `downloadUrl` to the APK asset URL

## Remaining setup (manual)

These are **not** done by the project files alone:

1. Push the CapCutMini repo / branch to GitHub (if not already).
2. Create a GitHub Release and attach the signed APK (`docs/GITHUB_RELEASES.md`).
3. Fill `downloadUrl` (and optional `sha256`) in `website/version.json`.
4. Connect Cloudflare Pages to the repo (or upload `website/`) and confirm the live URL.
5. Store the release keystore backup somewhere safe offline.
