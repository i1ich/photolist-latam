# ============================================================
# deploy.ps1  —  Rebuild and deploy PhotoList LATAM to AWS
# Run from the repository root in PowerShell.
# ============================================================

param(
    [string]$JavaHome = "",   # path to JDK 21; auto-detected if omitted
    [string]$Region   = "sa-east-1"
)

$Root = Split-Path $PSScriptRoot -Parent

# ── Ensure Java 21+ is on PATH ─────────────────────────────────────────────
# Priority: explicit -JavaHome > $env:JAVA_HOME (if 21+) > known local JDK paths
function Get-JavaMajorVersion {
    param([string]$JavaBin)
    try {
        $v = & "$JavaBin\java.exe" -version 2>&1 | Select-String 'version "(\d+)' | ForEach-Object { $_.Matches[0].Groups[1].Value }
        return [int]$v
    } catch { return 0 }
}

$resolvedJava = ""
if ($JavaHome -ne "") {
    $resolvedJava = $JavaHome
} else {
    # Check if current java is already 21+
    $currentJava = (Get-Command java -ErrorAction SilentlyContinue)
    if ($currentJava) {
        $ver = & java -version 2>&1 | Select-String 'version "(\d+)' | ForEach-Object { [int]$_.Matches[0].Groups[1].Value }
        if ($ver -ge 21) { $resolvedJava = "current" }
    }
    if ($resolvedJava -eq "") {
        # Well-known local JDK paths on this machine
        $candidates = @(
            "$env:USERPROFILE\.jdks\openjdk-21.0.2",
            "C:\Program Files\Eclipse Adoptium\jdk-21*",
            "C:\Program Files\Microsoft\jdk-21*",
            "C:\Program Files\Java\jdk-21*"
        )
        foreach ($c in $candidates) {
            $found = Resolve-Path $c -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found -and (Test-Path "$found\bin\java.exe")) {
                $resolvedJava = $found.Path; break
            }
        }
    }
}

if ($resolvedJava -ne "" -and $resolvedJava -ne "current") {
    $env:JAVA_HOME = $resolvedJava
    $env:PATH = "$resolvedJava\bin;$env:PATH"
    Write-Host "==> Java: using $resolvedJava"
} elseif ($resolvedJava -eq "") {
    Write-Warning "Java 21+ not found. CDK may fail. Pass -JavaHome <path> to specify."
}

$ErrorActionPreference = "Stop"

# ── 1. Rebuild analyze-photo (VisionService + CDK stack changed) ────────────
Write-Host ""
Write-Host "==> 1/3  Build analyze-photo..."
& "$Root\mvnw.cmd" -f "$Root\functions\analyze-photo\pom.xml" package -q -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Build analyze-photo failed" }
Write-Host "    OK  →  functions/analyze-photo/target/analyze-photo.jar"

# ── 2. Rebuild infrastructure (PhotolistApiStack changed) ──────────────────
Write-Host ""
Write-Host "==> 2/3  Build infrastructure CDK app..."
& "$Root\mvnw.cmd" -f "$Root\infrastructure\pom.xml" package -q -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Build infrastructure failed" }
Write-Host "    OK  →  infrastructure/target/photolist-infrastructure.jar"

# ── 3. CDK deploy ──────────────────────────────────────────────────────────
Write-Host ""
Write-Host "==> 3/3  CDK deploy --all (region: $Region)..."
Push-Location "$Root\infrastructure"
try {
    npx --yes aws-cdk@2 deploy --all --require-approval never
    if ($LASTEXITCODE -ne 0) { throw "CDK deploy failed" }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "Deploy complete!"
Write-Host ""
Write-Host "Stack outputs:"
foreach ($stack in @("PhotolistStorageStack","PhotolistApiStack","PhotolistFrontendStack")) {
    $out = aws cloudformation describe-stacks --region $Region --stack-name $stack --output json 2>$null | ConvertFrom-Json
    if ($out) {
        Write-Host "  [$stack]"
        $out.Stacks[0].Outputs | ForEach-Object {
            Write-Host "    $($_.OutputKey) = $($_.OutputValue)"
        }
    }
}
