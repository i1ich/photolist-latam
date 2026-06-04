# ============================================================
# setup-ssm-params.ps1
# Creates all SSM parameters required to run photolist-latam.
# Run from PowerShell with AWS credentials configured.
# ============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$OpenAiApiKey,

    [string]$Region = "sa-east-1",

    # OpenAI model — can be changed here without redeploying Lambda
    [string]$VisionModel = "gpt-4.1-nano",

    # Maximum number of tokens in the model response
    [int]$VisionMaxTokens = 300
)

$ErrorActionPreference = "Stop"

Write-Host "==> Region: $Region"
Write-Host ""

# ── 1. OpenAI API key (SecureString) ─────────────────────────────────────────
Write-Host "1/3  /photolist/openai-api-key  [SecureString]"
aws ssm put-parameter `
    --region $Region `
    --name   "/photolist/openai-api-key" `
    --value  $OpenAiApiKey `
    --type   "SecureString" `
    --overwrite `
    --description "OpenAI API key for photolist-latam vision Lambda"
if ($LASTEXITCODE -ne 0) { throw "Failed to write openai-api-key" }
Write-Host "    OK"

# ── 2. Vision model name (String, public config) ─────────────────────────────
Write-Host "2/3  /photolist/vision-model     [String] = $VisionModel"
aws ssm put-parameter `
    --region $Region `
    --name   "/photolist/vision-model" `
    --value  $VisionModel `
    --type   "String" `
    --overwrite `
    --description "OpenAI model used by photolist-latam vision Lambda"
if ($LASTEXITCODE -ne 0) { throw "Failed to write vision-model" }
Write-Host "    OK"

# ── 3. Vision max tokens (String, public config) ─────────────────────────────
Write-Host "3/3  /photolist/vision-max-tokens [String] = $VisionMaxTokens"
aws ssm put-parameter `
    --region $Region `
    --name   "/photolist/vision-max-tokens" `
    --value  "$VisionMaxTokens" `
    --type   "String" `
    --overwrite `
    --description "max_tokens sent to OpenAI by photolist-latam vision Lambda"
if ($LASTEXITCODE -ne 0) { throw "Failed to write vision-max-tokens" }
Write-Host "    OK"

Write-Host ""
Write-Host "All SSM parameters written successfully."
Write-Host ""
Write-Host "Verify:"
aws ssm get-parameters-by-path --region $Region --path "/photolist/" --with-decryption `
    --query "Parameters[*].{Name:Name,Type:Type,Value:Value}" --output table
