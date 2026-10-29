$ErrorActionPreference = 'Stop'

$repoRoot = $PSScriptRoot

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "=== $Name ===" -ForegroundColor Cyan
    & $Action

    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

if (-not $env:DB_PASSWORD) {
    # Integration tests override datasource credentials through Testcontainers.
    # This placeholder only prevents unresolved configuration during test startup.
    $env:DB_PASSWORD = 'verification-placeholder'
}

try {
    Push-Location $repoRoot
    Invoke-Step 'Docker / Testcontainers availability' { docker info --format 'Docker {{.ServerVersion}}' }
    Pop-Location

    Push-Location (Join-Path $repoRoot 'backend')
    Invoke-Step 'Backend tests' { mvn test }
    Pop-Location

    Push-Location (Join-Path $repoRoot 'frontend')
    Invoke-Step 'Frontend typecheck' { npm run typecheck }
    Invoke-Step 'Frontend lint' { npm run lint }
    Invoke-Step 'Frontend tests' { npm test }
    Invoke-Step 'Frontend build' { npm run build }
    Pop-Location

    Push-Location $repoRoot
    Invoke-Step 'Git diff whitespace check' { git diff --check }
    Pop-Location

    Write-Host ""
    Write-Host 'ALL VERIFICATION PASSED' -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host "VERIFICATION FAILED: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
finally {
    while ((Get-Location).Path -ne $repoRoot -and (Get-Location).Path.StartsWith($repoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        Pop-Location
    }
}
