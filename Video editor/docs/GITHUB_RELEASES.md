# GitHub Releases — CapCutMini APK

Use GitHub Releases to host each public APK. The download website reads `website/version.json` and should point `downloadUrl` at the Release asset.

**Do not upload keystore files or `keystore.properties` to GitHub.**

## One-time repo checklist

1. Push the CapCutMini project (including `website/` and this doc).
2. Confirm `.gitignore` excludes `keystore.properties`, `signing/`, `*.jks`, `*.keystore`, and `*.apk`.
3. Keep `applicationId` = `com.capcutmini.app` forever.
4. Sign every public APK with the **same** release keystore.

## Before each release

1. Bump versions in `app/build.gradle.kts`:
   - `versionCode` — integer, must increase every public APK (1 → 2 → 3 …)
   - `versionName` — user-facing string (e.g. `0.3.0` → `0.3.1`)
2. Update `website/version.json` to match (`versionName`, `versionCode`, `releasedAt`, `releaseNotes`, `apkFileName`).
3. Build the signed release APK (see `docs/DISTRIBUTION.md`).
4. Rename for clarity, e.g. `CapCutMini-0.3.0.apk`.
5. Optionally compute SHA-256 and put it in `version.json` → `sha256`.

## Upload a Release (GitHub website)

1. Open the repository on GitHub → **Releases** → **Draft a new release**.
2. **Choose a tag**, e.g. `v0.3.0` (create the tag on publish).
3. **Release title**, e.g. `CapCutMini 0.3.0`.
4. Paste release notes (same bullets as `version.json`).
5. Attach the signed APK under **Attach binaries**.
6. Publish the release.
7. Open the APK asset → copy the download URL (ends with the `.apk` filename).
8. Set `website/version.json` → `downloadUrl` to that URL.
9. Commit and push `version.json` (and any site tweaks) so Cloudflare Pages can pick it up after you connect Pages.

## Upload a Release (GitHub CLI)

```powershell
# From the CapCutMini project root, after assembleRelease
$ver = "0.3.0"
$apk = "app\build\outputs\apk\release\app-release.apk"
$named = "CapCutMini-$ver.apk"
Copy-Item $apk $named

gh release create "v$ver" $named `
  --title "CapCutMini $ver" `
  --notes-file RELEASE_NOTES.md
```

Then copy the asset URL from the release page into `website/version.json`.

## Update installs

Users who already installed CapCutMini can install the new APK **over** the old one if:

- `applicationId` is still `com.capcutmini.app`
- the APK is signed with the **same** release key
- `versionCode` is **higher** than the installed build

If the signing key changes, Android will refuse the update until the app is uninstalled (local projects are lost).
