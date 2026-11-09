# CapCutMini download website

Static site for Cloudflare Pages. Root directory for Pages: **`website`**.

## Local preview

Open `index.html` in a browser, or serve the folder:

```powershell
npx --yes serve .
```

The page loads `version.json` for version, release notes, and the APK `downloadUrl`.

## Cloudflare Pages (manual — not configured by this repo alone)

1. Push this project to GitHub (or upload the `website` folder).
2. In Cloudflare Dashboard → **Workers & Pages** → **Create** → **Pages**.
3. Connect the repo (or direct upload).
4. Build settings:
   - **Framework preset:** None
   - **Build command:** *(leave empty)*
   - **Build output directory:** `website` (if the repo root is the CapCutMini project)  
     or `/` if you connect only the `website` folder.
5. Deploy. Copy the `*.pages.dev` URL.

This repository does **not** claim Cloudflare is connected until you complete those steps.

## Updating the download button

After each GitHub Release, edit `version.json`:

- `versionName` / `versionCode` — match `app/build.gradle.kts`
- `downloadUrl` — direct URL to the Release APK asset
- `releaseNotes` — bullet strings shown on the site
- Optionally `sha256` of the APK for integrity checks later

Commit and push so Pages redeploys (or re-upload).
