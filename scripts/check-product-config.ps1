param(
    [string]$Root = (Get-Location).Path
)

$ErrorActionPreference = 'Stop'
$failures = New-Object 'System.Collections.Generic.List[string]'

function Add-Failure([string]$Message) {
    $failures.Add($Message) | Out-Null
}

try {
    $rootPath = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $Root).Path)
    $configPath = Join-Path $rootPath 'config/product.json'
    $schemaPath = Join-Path $rootPath 'config/product.schema.json'
    if (-not (Test-Path -LiteralPath $configPath -PathType Leaf)) { throw "Missing $configPath" }
    if (-not (Test-Path -LiteralPath $schemaPath -PathType Leaf)) { throw "Missing $schemaPath" }

    $config = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($config.schemaVersion -ne 1) { Add-Failure 'schemaVersion must be 1' }

    $applicationId = [string]$config.identity.applicationId
    if ($applicationId -notmatch '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$') {
        Add-Failure "Invalid applicationId '$applicationId'"
    }
    if ([string]::IsNullOrWhiteSpace([string]$config.identity.appName)) { Add-Failure 'identity.appName is required' }
    if ([int64]$config.version.code -lt 1) { Add-Failure 'version.code must be positive' }
    if ([string]::IsNullOrWhiteSpace([string]$config.version.name)) { Add-Failure 'version.name is required' }

    foreach ($property in @('privacyUrl', 'termsUrl')) {
        $url = [string]$config.legal.$property
        if ($url -notmatch '^https://') { Add-Failure "legal.$property must use HTTPS" }
    }

    $googlePath = Join-Path $rootPath 'app/google-services.json'
    $google = Get-Content -LiteralPath $googlePath -Raw -Encoding UTF8 | ConvertFrom-Json
    $firebasePackages = @($google.client | ForEach-Object { $_.client_info.android_client_info.package_name })
    if ($applicationId -notin $firebasePackages) {
        Add-Failure "google-services.json does not contain '$applicationId'"
    }

    $policyRelativePath = [string]$config.advertising.policyFile
    if ($policyRelativePath -ne 'config/ad_policy.json') {
        Add-Failure "advertising.policyFile must be 'config/ad_policy.json'"
    }
    $policyPath = [IO.Path]::GetFullPath((Join-Path $rootPath $policyRelativePath))
    if (-not $policyPath.StartsWith($rootPath, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'advertising.policyFile resolves outside the repository'
    }
    $policy = Get-Content -LiteralPath $policyPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($null -ne $policy.package_name) {
        Add-Failure 'canonical ad policy must not define package_name; the build generates it from identity.applicationId'
    }
    $areaKeys = @($policy.ad_units | ForEach-Object { [string]$_.areakey })
    if ($areaKeys.Count -eq 0) { Add-Failure 'canonical ad policy must define ad_units' }
    $duplicateAreaKeys = @($areaKeys | Group-Object | Where-Object { $_.Count -gt 1 } | ForEach-Object { $_.Name })
    if ($duplicateAreaKeys.Count -gt 0) {
        Add-Failure ("canonical ad policy contains duplicate area keys: {0}" -f ($duplicateAreaKeys -join ', '))
    }

    $nativeIds = @($config.advertising.admob.nativeIds)
    if ($nativeIds.Count -eq 0) { Add-Failure 'advertising.admob.nativeIds must not be empty' }
    foreach ($nativeId in $nativeIds) {
        foreach ($field in @('highPriceID', 'midPriceID', 'lowPriceID')) {
            if ([string]::IsNullOrWhiteSpace([string]$nativeId.$field)) {
                Add-Failure "advertising.admob.nativeIds.$field is required"
            }
        }
    }

    foreach ($legacyRaw in @('ad_policy.json', 'native_ad_ids.json')) {
        $legacyPath = Join-Path $rootPath ("app/src/main/res/raw/{0}" -f $legacyRaw)
        if (Test-Path -LiteralPath $legacyPath -PathType Leaf) {
            Add-Failure "$legacyPath duplicates generated product configuration"
        }
    }

    $stringsPath = Join-Path $rootPath 'app/src/main/res/values/strings.xml'
    $strings = Get-Content -LiteralPath $stringsPath -Raw -Encoding UTF8
    foreach ($name in @('app_name', 'app_profile_key', 'app_theme_key', 'terms_of_service_url', 'privacy_policy_url')) {
        if ($strings -match ('name\s*=\s*["'']' + [regex]::Escape($name) + '["'']')) {
            Add-Failure "strings.xml still defines generated product resource '$name'"
        }
    }

    $appBuildPath = Join-Path $rootPath 'app/build.gradle.kts'
    $appBuild = Get-Content -LiteralPath $appBuildPath -Raw -Encoding UTF8
    if ($appBuild -notmatch 'id\("quickclean\.product-config"\)') {
        Add-Failure 'app does not apply quickclean.product-config'
    }
    if ($appBuild -match 'signingConfig\s*=\s*signingConfigs\.getByName\("debug"\)') {
        Add-Failure 'release build still uses debug signing'
    }

    if ($failures.Count -gt 0) {
        Write-Host 'Product config check failed:'
        $failures | ForEach-Object { Write-Host "- $_" }
        exit 1
    }

    Write-Host "Product config check passed for '$applicationId' version $($config.version.name) ($($config.version.code))."
    exit 0
} catch {
    Write-Host ('Product config check could not complete: ' + $_.Exception.Message)
    exit 2
}
