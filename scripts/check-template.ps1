param(
    [string]$ExpectedProfileKey = ""
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$configPath = Join-Path $root "config/product.json"
$manifestPath = Join-Path $root "app/src/main/AndroidManifest.xml"
$firebasePath = Join-Path $root "app/google-services.json"
$sourcePath = Join-Path $root "app/src/main"

$config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$applicationId = $config.identity.applicationId
$profileKey = $config.identity.profileKey

if ($ExpectedProfileKey -and $profileKey -ne $ExpectedProfileKey) {
    throw "Expected profileKey '$ExpectedProfileKey', found '$profileKey'."
}

$firebase = Get-Content -LiteralPath $firebasePath -Raw | ConvertFrom-Json
$firebasePackages = @($firebase.client | ForEach-Object {
    $_.client_info.android_client_info.package_name
})
if ($applicationId -notin $firebasePackages) {
    throw "google-services.json does not contain applicationId '$applicationId'."
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw
$componentNames = [regex]::Matches($manifest, 'android:name="(com\.quickcleanpro\.phonecleaner[^"]+)"') |
    ForEach-Object { $_.Groups[1].Value }
$sources = Get-ChildItem -LiteralPath (Join-Path $sourcePath "java") -Recurse -Filter *.kt |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }
foreach ($component in $componentNames) {
    $className = $component.Substring($component.LastIndexOf('.') + 1)
    if (-not ($sources -match "(class|object)\s+$className\b")) {
        throw "Manifest component has no matching Kotlin class: $component"
    }
}

$requiredIcons = @(
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"
)
foreach ($relativePath in $requiredIcons) {
    if (-not (Test-Path -LiteralPath (Join-Path $root $relativePath))) {
        throw "Missing launcher resource: $relativePath"
    }
}

$oldPackages = Get-ChildItem -LiteralPath $sourcePath -Recurse -File -Include *.kt,*.xml |
    Select-String -Pattern 'com\.quickcleanpro\.phonecleaner\.use\.(app|core|feature|skin)'
if ($oldPackages) {
    throw "Old package references remain:`n$($oldPackages -join "`n")"
}

Write-Host "Template check passed: $applicationId ($profileKey)"
