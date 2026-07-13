param(
    [string]$Root,
    [string]$ExpectedApplicationId,
    [string[]]$ForbiddenApplicationIds = @(),
    [string[]]$ForbiddenBrandTerms = @(),
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'

function Resolve-RepositoryRoot([string]$RequestedRoot) {
    $candidate = $RequestedRoot
    if ([string]::IsNullOrWhiteSpace($candidate)) {
        $candidate = Join-Path (Split-Path -Parent $PSScriptRoot) '.'
    }
    if (-not (Test-Path -LiteralPath $candidate -PathType Container)) {
        throw "Repository root does not exist: $candidate"
    }
    return [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $candidate).Path)
}

function Add-Failure([string]$Message) {
    $script:Failures.Add($Message) | Out-Null
}

function Relative-Path([string]$Path) {
    $normalizedPath = $Path.Replace('\', '/')
    $normalizedRoot = $RepositoryRoot.Replace('\', '/').TrimEnd('/')
    if ($normalizedPath.StartsWith($normalizedRoot + '/', [StringComparison]::OrdinalIgnoreCase)) {
        return $normalizedPath.Substring($normalizedRoot.Length + 1)
    }
    return $normalizedPath
}

function Read-Json([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $null
    }
    return Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | ConvertFrom-Json
}

function Find-ApplicationId([string]$BuildFile) {
    if (-not (Test-Path -LiteralPath $BuildFile -PathType Leaf)) {
        return $null
    }
    $content = Get-Content -LiteralPath $BuildFile -Raw -Encoding UTF8
    $match = [regex]::Match($content, 'applicationId\s*=\s*"([^"]+)"')
    if ($match.Success) { return $match.Groups[1].Value }
    return $null
}

function Search-ForbiddenTerm([string]$Term, [string]$Label) {
    if ([string]::IsNullOrWhiteSpace($Term)) { return }

    $roots = @(
        (Join-Path $RepositoryRoot 'app/src'),
        (Join-Path $RepositoryRoot 'config'),
        (Join-Path $RepositoryRoot 'gradle')
    ) | Where-Object { Test-Path -LiteralPath $_ -PathType Container }

    foreach ($searchRoot in $roots) {
        $files = Get-ChildItem -LiteralPath $searchRoot -Recurse -File -ErrorAction Stop |
            Where-Object {
                $_.Extension -in @('.kt', '.java', '.xml', '.json', '.properties', '.kts') -and
                $_.FullName.Replace('\', '/') -notmatch '/(build|test|androidTest)/'
            }
        foreach ($file in $files) {
            $matches = @(Select-String -LiteralPath $file.FullName -SimpleMatch -Pattern $Term -CaseSensitive:$false)
            foreach ($match in $matches) {
                Add-Failure ("{0}:{1}: forbidden {2} '{3}'" -f (Relative-Path $file.FullName), $match.LineNumber, $Label, $Term)
            }
        }
    }
}

try {
    $Failures = New-Object 'System.Collections.Generic.List[string]'
    $RepositoryRoot = Resolve-RepositoryRoot $Root

    if ([string]::IsNullOrWhiteSpace($ExpectedApplicationId)) {
        $productConfig = Read-Json (Join-Path $RepositoryRoot 'config/product.json')
        if ($null -ne $productConfig -and $null -ne $productConfig.identity) {
            $ExpectedApplicationId = [string]$productConfig.identity.applicationId
        }
    }
    if ([string]::IsNullOrWhiteSpace($ExpectedApplicationId)) {
        $ExpectedApplicationId = Find-ApplicationId (Join-Path $RepositoryRoot 'app/build.gradle.kts')
    }
    if ([string]::IsNullOrWhiteSpace($ExpectedApplicationId)) {
        throw 'Expected applicationId could not be determined. Pass -ExpectedApplicationId explicitly.'
    }

    $googleServices = Read-Json (Join-Path $RepositoryRoot 'app/google-services.json')
    if ($null -eq $googleServices) {
        Add-Failure 'app/google-services.json is missing or unreadable'
    } else {
        $firebasePackages = @($googleServices.client | ForEach-Object { $_.client_info.android_client_info.package_name })
        if ($ExpectedApplicationId -notin $firebasePackages) {
            Add-Failure ("app/google-services.json does not contain expected package '{0}'" -f $ExpectedApplicationId)
        }
    }

    $adPolicy = Read-Json (Join-Path $RepositoryRoot 'config/ad_policy.json')
    if ($null -eq $adPolicy) {
        Add-Failure 'config/ad_policy.json is missing or unreadable'
    } elseif ($null -ne $adPolicy.package_name) {
        Add-Failure 'config/ad_policy.json must not hardcode package_name'
    }

    foreach ($applicationId in $ForbiddenApplicationIds) {
        Search-ForbiddenTerm $applicationId 'applicationId'
    }
    foreach ($brandTerm in $ForbiddenBrandTerms) {
        Search-ForbiddenTerm $brandTerm 'brand term'
    }

    if ($Strict) {
        $manifest = Join-Path $RepositoryRoot 'app/src/main/AndroidManifest.xml'
        if (Test-Path -LiteralPath $manifest -PathType Leaf) {
            $hardcodedPackages = @(Select-String -LiteralPath $manifest -Pattern 'com\.quickcleanpro\.phonecleaner' -CaseSensitive:$false)
            foreach ($match in $hardcodedPackages) {
                if ($match.Line -match 'android:name=') { continue }
                Add-Failure ("{0}:{1}: hardcoded package outside component class name" -f (Relative-Path $manifest), $match.LineNumber)
            }
        }
    }

    if ($Failures.Count -gt 0) {
        Write-Host 'Brand and package consistency check failed:'
        Write-Host ''
        $Failures | ForEach-Object { Write-Host $_ }
        exit 1
    }

    Write-Host ("Brand and package consistency check passed for '{0}'." -f $ExpectedApplicationId)
    exit 0
} catch {
    Write-Host ('Brand and package consistency check could not complete: ' + $_.Exception.Message)
    exit 2
}
