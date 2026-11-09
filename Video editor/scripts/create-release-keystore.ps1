# Creates a local CapCutMini release keystore + keystore.properties (both gitignored).
# Run once from the project root. Back up signing/ and keystore.properties forever —
# losing the key means users cannot update without uninstalling.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$signingDir = Join-Path $root "signing"
$keystorePath = Join-Path $signingDir "capcutmini-release.jks"
$propsPath = Join-Path $root "keystore.properties"

if (Test-Path $keystorePath) {
    Write-Host "Keystore already exists: $keystorePath"
    Write-Host "Refusing to overwrite. Delete it manually only if you understand the update impact."
    exit 1
}

New-Item -ItemType Directory -Force -Path $signingDir | Out-Null

$keytool = $null
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\keytool.exe"
    if (Test-Path $candidate) { $keytool = $candidate }
}
if (-not $keytool) {
    $cmd = Get-Command keytool -ErrorAction SilentlyContinue
    if ($cmd) { $keytool = $cmd.Source }
}
if (-not $keytool) {
    throw "keytool not found. Set JAVA_HOME to a JDK 17+ install."
}

# Random passwords local only — printed once to console; stored in gitignored properties.
$storePassword = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
$keyPassword = $storePassword
$alias = "capcutmini"

& $keytool -genkeypair -v `
    -storetype PKCS12 `
    -keystore $keystorePath `
    -alias $alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $storePassword `
    -keypass $keyPassword `
    -dname "CN=CapCutMini, OU=Mobile, O=CapCutMini, C=US"

@"
storeFile=signing/capcutmini-release.jks
storePassword=$storePassword
keyAlias=$alias
keyPassword=$keyPassword
"@ | Set-Content -Path $propsPath -Encoding ASCII

Write-Host ""
Write-Host "Created:"
Write-Host "  $keystorePath"
Write-Host "  $propsPath"
Write-Host ""
Write-Host "IMPORTANT: Back up signing/ and keystore.properties offline. Do not commit them."
