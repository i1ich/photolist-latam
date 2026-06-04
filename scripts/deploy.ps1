# ============================================================
# deploy.ps1  —  Rebuild and deploy PhotoList LATAM to AWS
# Run from the repository root in PowerShell.
# ============================================================

param(
    [string]$JavaHome = "",   # path to JDK 21, if java is not on PATH
    [string]$Region   = "sa-east-1"
)

$Root = Split-Path $PSScriptRoot -Parent

# ── Optionally switch to Java 21 ───────────────────────────────────────────
if ($JavaHome -ne "") {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
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
