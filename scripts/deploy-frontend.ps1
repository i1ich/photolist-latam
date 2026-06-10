# deploy-frontend.ps1
# Run from the repository root in PowerShell.

$ErrorActionPreference = "Stop"
$Region = "sa-east-1"

# ── Fetch parameters from CloudFormation ─────────────────────────────────────
Write-Host "Fetching stack outputs..."
$stack = aws cloudformation describe-stacks `
  --stack-name PhotolistFrontendStack `
  --region $Region `
  --output json | ConvertFrom-Json

$outputs = @{}
$stack.Stacks[0].Outputs | ForEach-Object { $outputs[$_.OutputKey] = $_.OutputValue }

$Bucket  = $outputs["FrontendBucketName"]
$DistId  = $outputs["FrontendDistributionId"]
$SiteUrl = $outputs["FrontendUrl"]

Write-Host "  Bucket : $Bucket"
Write-Host "  Dist   : $DistId"
Write-Host "  URL    : $SiteUrl"

# ── Build frontend ────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "Building frontend..."
Push-Location "$PSScriptRoot\..\frontend"
try {
    # .env.local overrides .env.production in Vite — hide it during the build
    if (Test-Path ".env.local") { Rename-Item ".env.local" ".env.local.bak" }

    npm ci
    npm run build

    if (Test-Path ".env.local.bak") { Rename-Item ".env.local.bak" ".env.local" }
} catch {
    if (Test-Path ".env.local.bak") { Rename-Item ".env.local.bak" ".env.local" }
    throw
}
Pop-Location

# ── Sync to S3 ────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "Syncing to S3..."
aws s3 sync "$PSScriptRoot\..\frontend\dist" "s3://$Bucket/" --delete --region $Region

# ── Invalidate CloudFront cache ───────────────────────────────────────────────
Write-Host ""
Write-Host "Invalidating CloudFront cache..."
aws cloudfront create-invalidation `
  --distribution-id $DistId `
  --paths "/*"

Write-Host ""
Write-Host "Done! Site: $SiteUrl"