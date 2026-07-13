param(
    [string]$Root = (Get-Location).Path
)

$ErrorActionPreference = 'Stop'
Set-Location $Root

$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure {
    param([string]$Message)
    $failures.Add($Message) | Out-Null
}

function Invoke-RgCheck {
    param(
        [string]$Title,
        [string]$Pattern,
        [string[]]$Paths,
        [string[]]$ExtraArgs = @()
    )

    $existingPaths = @($Paths | Where-Object { Test-Path -LiteralPath $_ })
    if ($existingPaths.Count -eq 0) { return }

    $args = @('-n', $Pattern) + $existingPaths + $ExtraArgs
    $output = & rg @args 2>$null
    if ($LASTEXITCODE -eq 0) {
        Add-Failure ($Title + ":`n" + ($output -join "`n"))
    } elseif ($LASTEXITCODE -gt 1) {
        Add-Failure ($Title + ": rg failed with exit code " + $LASTEXITCODE)
    }
}

$sourceRoot = 'app/src/main/java/com/quickcleanpro/phonecleaner/use'

foreach ($legacyDir in @('view', 'viewmodel', 'notification', 'theme')) {
    $path = Join-Path $sourceRoot $legacyDir
    if (Test-Path -LiteralPath $path) {
        Add-Failure "Legacy top-level directory still exists: $path"
    }
}

Invoke-RgCheck `
    -Title 'Old package references remain' `
    -Pattern 'use\.(view|viewmodel|notification|theme)|core\.(route|advertise|config|di)' `
    -Paths @('app/src/main/java', 'app/src/test/java', 'app/src/androidTest/java') `
    -ExtraArgs @('-g', '*.kt', '-g', '*.java', '-g', '*.xml')

Invoke-RgCheck `
    -Title 'feature layer imports skin' `
    -Pattern 'import\s+com\.quickcleanpro\.phonecleaner\.use\.skin\.' `
    -Paths @('app/src/main/java/com/quickcleanpro/phonecleaner/use/feature') `
    -ExtraArgs @('-g', '*.kt', '-g', '*.java')

Invoke-RgCheck `
    -Title 'feature layer imports app' `
    -Pattern 'import\s+com\.quickcleanpro\.phonecleaner\.use\.app\.' `
    -Paths @('app/src/main/java/com/quickcleanpro/phonecleaner/use/feature') `
    -ExtraArgs @('-g', '*.kt', '-g', '*.java')

Invoke-RgCheck `
    -Title 'core layer imports upper layers' `
    -Pattern 'import\s+com\.quickcleanpro\.phonecleaner\.use\.(feature|skin|app)\.' `
    -Paths @('app/src/main/java/com/quickcleanpro/phonecleaner/use/core') `
    -ExtraArgs @('-g', '*.kt', '-g', '*.java')

if ($failures.Count -gt 0) {
    Write-Host 'Variant boundary check failed:'
    Write-Host ''
    foreach ($failure in $failures) {
        Write-Host $failure
        Write-Host ''
    }
    exit 1
}

Write-Host 'Variant boundary check passed.'