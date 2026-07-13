param(
    [string]$Root,
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'

function Fail([string]$Message) {
    $script:Failures.Add($Message) | Out-Null
}

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

function Get-SourceFiles([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        return @()
    }
    return @(Get-ChildItem -LiteralPath $Path -Recurse -File -ErrorAction Stop |
        Where-Object { $_.Extension -in @('.kt', '.java') })
}

function Is-FutureFeatureFile([string]$Path) {
    $normalized = $Path.Replace('\', '/')
    return ($normalized -match '/feature/' -and $normalized -notmatch '/use/feature/')
}

function Is-FutureCoreFile([string]$Path) {
    $normalized = $Path.Replace('\', '/')
    return ($normalized -match '/core/' -and $normalized -notmatch '/use/core/')
}

function Relative-Path([string]$Path) {
    $normalizedPath = $Path.Replace('\', '/')
    $normalizedRoot = $RepositoryRoot.Replace('\', '/').TrimEnd('/')
    if ($normalizedPath.StartsWith($normalizedRoot + '/', [StringComparison]::OrdinalIgnoreCase)) {
        return $normalizedPath.Substring($normalizedRoot.Length + 1)
    }
    return $normalizedPath
}

function Check-Imports {
    param(
        [System.IO.FileInfo]$File,
        [string]$Pattern,
        [string]$Rule
    )

    $matches = @(Select-String -LiteralPath $File.FullName -Pattern $Pattern -CaseSensitive:$false)
    foreach ($match in $matches) {
        Fail ("{0}: {1} (line {2})`n  {3}" -f (Relative-Path $File.FullName), $Rule, $match.LineNumber, $match.Line.Trim())
    }
}

try {
    $Failures = New-Object 'System.Collections.Generic.List[string]'
    $RepositoryRoot = Resolve-RepositoryRoot $Root

    $javaRoot = Join-Path $RepositoryRoot 'app/src/main/java'
    $allAppSourceFiles = @(Get-SourceFiles $javaRoot)
    $legacyFeatureFiles = @($allAppSourceFiles | Where-Object {
        $_.FullName.Replace('\', '/') -match '/use/feature/'
    })
    $futureFeatureFiles = @(Get-SourceFiles $javaRoot | Where-Object { Is-FutureFeatureFile $_.FullName })
    $futureCoreFiles = @()

    $topLevelCore = Join-Path $RepositoryRoot 'core'
    if (Test-Path -LiteralPath $topLevelCore -PathType Container) {
        $futureCoreFiles += Get-SourceFiles $topLevelCore
    }
    $futureCoreFiles += @(Get-SourceFiles $javaRoot | Where-Object { Is-FutureCoreFile $_.FullName })

    $thirdPartySdkPattern = '^\s*import\s+(com\.pdffox\.adv\.|com\.google\.android\.gms\.ads\.|com\.google\.firebase\.|com\.facebook\.|com\.pangle\.|com\.tiktok\.|com\.singular\.|thinkingdata\.|com\.trustlook\.)'

    # These rules cover the current production tree, not only the target directory layout.
    $currentUiPresentationFiles = @($allAppSourceFiles | Where-Object {
        $normalized = $_.FullName.Replace('\', '/')
        $normalized -match '/(use/skin|presentation)/' -and $normalized -notmatch '/data/'
    })
    foreach ($file in $currentUiPresentationFiles) {
        Check-Imports $file '^\s*import\s+.*RepositoryImpl\b' 'UI/presentation imports a concrete repository'
        Check-Imports $file '^\s*import\s+.*\.service\.' 'UI/presentation imports a Service implementation'
        Check-Imports $file '^\s*import\s+.*(SharedPreferencesUtils|GlobalContext)\b' 'UI/presentation accesses global storage or DI'
        Check-Imports $file '^\s*import\s+com\.quickcleanpro\.phonecleaner\.app\.monetization\.(?!AdPrivacyGateway\b)' 'UI/presentation imports a monetization adapter implementation'
        Check-Imports $file $thirdPartySdkPattern 'UI/presentation imports a third-party SDK directly'
    }

    # Legacy packages are still production code. Keep the default gate honest while
    # the directory migration is in progress instead of checking only future paths.
    foreach ($file in $legacyFeatureFiles) {
        Check-Imports $file '^\s*import\s+com\.quickcleanpro\.phonecleaner\.use\.app\.' 'feature imports app runtime'
        Check-Imports $file '^\s*import\s+com\.quickcleanpro\.phonecleaner\.use\.skin\.' 'feature imports skin UI'
    }

    $screenFiles = @($allAppSourceFiles | Where-Object { $_.Name -like '*Screen.kt' })
    foreach ($file in $screenFiles) {
        Check-Imports $file '^\s*import\s+org\.koin\.' 'Screen resolves dependencies directly; inject them in its Route'
        Check-Imports $file '^\s*import\s+.*Repository\b' 'Screen imports a repository contract'
        Check-Imports $file '^\s*import\s+.*(LocalRouter|LocalFeatureOperationTracker|LocalInterstitialAdInterceptor|LocalCleanXPermissionCoordinator)\b' 'Screen accesses a business CompositionLocal'
    }

    # Add a Screen here once its Route owns state collection and platform effects.
    # This incremental allowlist prevents migrated pages from drifting backwards while
    # legacy pages are converted feature by feature.
    $pureScreenRelativePaths = @(
        'com/quickcleanpro/phonecleaner/use/skin/home/HomeScreen.kt',
        'com/quickcleanpro/phonecleaner/use/skin/settings/SettingsScreen.kt',
        'com/quickcleanpro/phonecleaner/use/skin/toolbox/battery/BatteryInfoScreen.kt',
        'com/quickcleanpro/phonecleaner/use/skin/toolbox/deviceinfo/DeviceInfoScreen.kt',
        'com/quickcleanpro/phonecleaner/use/skin/toolbox/networkscan/NetworkScanScreen.kt',
        'com/quickcleanpro/phonecleaner/use/skin/toolbox/networkspeed/NetworkSpeedScreen.kt'
    )
    foreach ($relativePath in $pureScreenRelativePaths) {
        $screenPath = Join-Path $javaRoot $relativePath
        if (-not (Test-Path -LiteralPath $screenPath -PathType Leaf)) {
            Fail ("Pure Screen contract file is missing: {0}" -f $relativePath)
            continue
        }
        $screenFile = Get-Item -LiteralPath $screenPath
        Check-Imports $screenFile '^\s*import\s+.*AppNavigator\b' 'Pure Screen imports AppNavigator; pass a semantic callback from its Route'
        Check-Imports $screenFile '^\s*import\s+.*ViewModel\b' 'Pure Screen imports a ViewModel; collect state in its Route'
        Check-Imports $screenFile '^\s*import\s+.*Repository\b' 'Pure Screen imports a repository; resolve it in its Route'
        Check-Imports $screenFile '^\s*import\s+android\.app\.(Activity|Service)\b' 'Pure Screen imports a platform component'
        Check-Imports $screenFile '^\s*import\s+androidx\.compose\.ui\.platform\.LocalContext\b' 'Pure Screen accesses LocalContext; perform the platform action in its Route'
        Check-Imports $screenFile '^\s*import\s+androidx\.compose\.runtime\.(LaunchedEffect|DisposableEffect)\b' 'Pure Screen owns a side effect; move it to its Route'
    }

    # AppRoot stays a thin mount point. Runtime state and side-effect wiring belong in app/runtime.
    $appRootFile = Join-Path $javaRoot 'com/quickcleanpro/phonecleaner/use/app/AppRoot.kt'
    if (-not (Test-Path -LiteralPath $appRootFile -PathType Leaf)) {
        Fail 'app/AppRoot.kt is missing; keep a stable thin application mount point'
    } else {
        $appRootLines = @(Get-Content -LiteralPath $appRootFile)
        if ($appRootLines.Count -gt 120) {
            Fail ("{0}: AppRoot exceeds the 120-line mount-point limit ({1} lines)" -f (Relative-Path $appRootFile), $appRootLines.Count)
        }
        $stateMachineMarkers = @(Select-String -LiteralPath $appRootFile -Pattern 'mutableStateOf|mutableIntStateOf|LaunchedEffect|DisposableEffect|rememberNavController|koinInject|koinViewModel|AdvertiseSdkAdapter|RepositoryImpl')
        foreach ($marker in $stateMachineMarkers) {
            Fail ("{0}: AppRoot contains runtime state/side-effect wiring (line {1})`n  {2}" -f (Relative-Path $appRootFile), $marker.LineNumber, $marker.Line.Trim())
        }
    }

    # SDK imports are restricted to monetization adapters. Trustlook is a feature-owned antivirus data adapter.
    foreach ($file in $allAppSourceFiles) {
        $normalized = $file.FullName.Replace('\', '/')
        $allowedMonetizationAdapter =
            $normalized -match '/(app|core)/monetization/' -or
            $normalized -match '/feature/antivirus/data/'
        if (-not $allowedMonetizationAdapter) {
            Check-Imports $file $thirdPartySdkPattern 'third-party SDK imported outside an approved adapter'
        }
    }

    # The new layout keeps domain code free of Android and UI dependencies.
    $domainFiles = @($futureFeatureFiles | Where-Object {
        $_.FullName.Replace('\', '/') -match '/domain/'
    })
    foreach ($file in $domainFiles) {
        Check-Imports $file '^\s*import\s+(android|androidx)\.' 'domain imports Android APIs'
        Check-Imports $file '^\s*import\s+.*\.(presentation|ui|data)\.' 'domain imports an upper/data layer'
    }

    # Presentation and UI must depend on domain contracts, not data implementations.
    $presentationFiles = @($futureFeatureFiles | Where-Object {
        $_.FullName.Replace('\', '/') -match '/(presentation|ui)/'
    })
    foreach ($file in $presentationFiles) {
        Check-Imports $file '^\s*import\s+.*\.data\.' 'presentation/ui imports data directly'
        Check-Imports $file '^\s*import\s+(android\.content\.SharedPreferences|androidx\.preference\.)' 'presentation/ui accesses preferences directly'
    }

    # Core modules are below app and features. Activity/Service ownership stays at the app/platform edge.
    foreach ($file in $futureCoreFiles) {
        Check-Imports $file '^\s*import\s+com\.quickcleanpro\.phonecleaner\.(app|feature)\.' 'core imports app/feature'
        Check-Imports $file '^\s*import\s+com\.quickcleanpro\.phonecleaner\.use\.(app|feature|service)\.' 'new core code imports legacy upper layers'
        Check-Imports $file '^\s*import\s+com\.quickcleanpro\.phonecleaner\.use\.skin\.(?!common\.theme\.)' 'new core code imports legacy upper layers'
        Check-Imports $file '^\s*import\s+android\.app\.(Activity|Service)\b' 'core imports Activity/Service'
    }

    # Third-party SDKs belong in core/monetization adapters once the new tree exists.
    foreach ($file in @($futureFeatureFiles + $futureCoreFiles)) {
        $normalized = $file.FullName.Replace('\', '/')
        if ($normalized -notmatch '/core/monetization/') {
            Check-Imports $file $thirdPartySdkPattern 'third-party SDK imported outside core/monetization'
        }
    }

    if ($Strict) {
        # During migration this catches legacy domain files too. The default mode intentionally
        # checks only the new tree so the guard can be enabled before files are moved.
        $legacyDomainFiles = @(Get-SourceFiles $javaRoot | Where-Object {
            $_.FullName.Replace('\', '/') -match '/domain/'
        })
        foreach ($file in $legacyDomainFiles) {
            Check-Imports $file '^\s*import\s+(android|androidx)\.' 'strict domain imports Android APIs'
            Check-Imports $file '^\s*import\s+.*\.(presentation|ui|data)\.' 'strict domain imports an upper/data layer'
        }
    }

    # Validate project-to-project dependencies for physical core modules when present.
    $gradleFiles = @(Get-ChildItem -LiteralPath $RepositoryRoot -Recurse -File -ErrorAction Stop |
        Where-Object {
            $_.Name -in @('build.gradle', 'build.gradle.kts') -and
            $_.FullName.Replace('\', '/') -notmatch '/(\.gradle|build|docs|scripts)/'
        })

    foreach ($gradleFile in $gradleFiles) {
        $normalized = $gradleFile.FullName.Replace('\', '/')
        $module = $null
        if ($normalized -match '/app/build\.gradle(?:\.kts)?$') {
            $module = ':app'
        } elseif ($normalized -match '/core/([^/]+)/build\.gradle(?:\.kts)?$') {
            $module = ':core:' + $Matches[1]
        } elseif ($normalized -match '/feature/([^/]+)/build\.gradle(?:\.kts)?$') {
            $module = ':feature:' + $Matches[1]
        }
        if ($null -eq $module) { continue }

        $content = Get-Content -LiteralPath $gradleFile.FullName -Raw -ErrorAction Stop
        $projectMatches = [regex]::Matches($content, 'project\s*\(\s*["''](:[^"'']+)["'']\s*\)')
        foreach ($projectMatch in $projectMatches) {
            $dependency = $projectMatch.Groups[1].Value
            if ($dependency -eq $module) {
                Fail ("{0}: module depends on itself ({1})" -f (Relative-Path $gradleFile.FullName), $module)
            }
            if ($module -like ':core:*' -and $module -ne ':core:model' -and $dependency -notin @(':core:model')) {
                Fail ("{0}: {1} may depend only on :core:model, found {2}" -f (Relative-Path $gradleFile.FullName), $module, $dependency)
            }
            if ($module -eq ':core:model') {
                Fail ("{0}: :core:model must not depend on another project ({1})" -f (Relative-Path $gradleFile.FullName), $dependency)
            }
        }
    }

    if ($Failures.Count -gt 0) {
        Write-Host 'Dependency boundary check failed:'
        Write-Host ''
        foreach ($failure in $Failures) {
            Write-Host $failure
            Write-Host ''
        }
        exit 1
    }

    $mode = if ($Strict) { 'strict' } else { 'default' }
    Write-Host ("Dependency boundary check passed ({0} mode)." -f $mode)
    exit 0
} catch {
    Write-Host ('Dependency boundary check could not complete: ' + $_.Exception.Message)
    exit 2
}
