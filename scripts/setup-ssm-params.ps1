# ============================================================
# setup-ssm-params.ps1
# Creates all SSM parameters required to run photolist-latam.
# Run from PowerShell with AWS credentials configured.
# ============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$OpenAiApiKey,

    [Parameter(Mandatory=$true)]
    [string]$MlClientId,

    [Parameter(Mandatory=$true)]
    [string]$MlClientSecret,

    [Parameter(Mandatory=$true)]
    [string]$MlRefreshToken,

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

# ── 4. MercadoLibre client_id (String, not secret) ───────────────────────────
Write-Host "4/6  /photolist/ml/client_id  [String]"
aws ssm put-parameter `
    --region $Region `
    --name   "/photolist/ml/client_id" `
    --value  $MlClientId `
    --type   "String" `
    --overwrite `
    --description "MercadoLibre OAuth app client_id"
if ($LASTEXITCODE -ne 0) { throw "Failed to write ml/client_id" }
Write-Host "    OK"

# ── 5. MercadoLibre client_secret (SecureString) ─────────────────────────────
Write-Host "5/6  /photolist/ml/client_secret  [SecureString]"
aws ssm put-parameter `
    --region $Region `
    --name   "/photolist/ml/client_secret" `
    --value  $MlClientSecret `
    --type   "SecureString" `
    --overwrite `
    --description "MercadoLibre OAuth app client_secret"
if ($LASTEXITCODE -ne 0) { throw "Failed to write ml/client_secret" }
Write-Host "    OK"

# ── 6. MercadoLibre refresh_token (SecureString, rotates on every API call) ──
Write-Host "6/6  /photolist/ml/refresh_token  [SecureString]"
aws ssm put-parameter `
    --region $Region `
    --name   "/photolist/ml/refresh_token" `
    --value  $MlRefreshToken `
    --type   "SecureString" `
    --overwrite `
    --description "MercadoLibre OAuth refresh_token (rotated by Lambda on every use)"
if ($LASTEXITCODE -ne 0) { throw "Failed to write ml/refresh_token" }
Write-Host "    OK"

Write-Host ""
Write-Host "All SSM parameters written successfully."
Write-Host ""
Write-Host "Verify:"
aws ssm get-parameters-by-path --region $Region --path "/photolist/" --with-decryption `
    --query "Parameters[*].{Name:Name,Type:Type,Value:Value}" --output table
