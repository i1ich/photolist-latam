# Deployment Guide — PhotoList LATAM (MVP)

This guide takes you from a clean machine to a live URL. The whole thing runs on **one AWS
account** (single bill) and stays inside the AWS free tier at MVP traffic.

| Layer | Hosting | Recurring cost at MVP scale |
|-------|---------|-----------------------------|
| Backend (API, Lambdas, storage) | AWS Lambda + API Gateway + S3 + DynamoDB — serverless | ~$0 (free tier) |
| Frontend (PWA) | AWS S3 + CloudFront — static + CDN | ~$0 (free tier) |
| Vision LLM | OpenAI API (pay per call) | ~$5–15 / month |
| Domain (optional) | Registrar | ~$10–15 / year |

There is **no always-on server** on either side, so you are not paying for two servers —
both layers bill per request and sit at roughly zero for MVP volumes.

---

## 0. Prerequisites (install once)

| Tool | Why | Check |
|------|-----|-------|
| **Java 21** (JDK) | Build Lambdas + run CDK app | `java -version` → `21.x` |
| **Node.js 20+** | Run the CDK CLI and build the frontend | `node -v` |
| **AWS CLI v2** | Auth, secrets, frontend sync | `aws --version` |
| **Git** | Clone the repo | `git --version` |

> Maven is **not** required — the repo ships the Maven Wrapper (`mvnw` / `mvnw.cmd`).

### Java 21 must be the `java` on your PATH for the deploy step

The CDK CLI launches the infrastructure app with whatever `java` resolves to on your `PATH`
(it ignores `JAVA_HOME`). If `java -version` shows anything other than 21, point your PATH at
the JDK 21 for this shell:

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Users\<you>\.jdks\openjdk-21.0.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version   # must say 21
```

**Linux / macOS:**
```bash
export JAVA_HOME=~/.jdks/openjdk-21.0.2
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # must say 21
```

---

## 1. Create an AWS account and credentials

1. Create an AWS account at <https://aws.amazon.com> (free tier covers this project).
2. In the **IAM** console, create a user (or an Identity Center user) with **AdministratorAccess**
   for the initial deploy. Create an access key for it.
3. Configure the CLI:
   ```bash
   aws configure
   # AWS Access Key ID:     ...
   # AWS Secret Access Key: ...
   # Default region name:   sa-east-1     <-- must be sa-east-1 (São Paulo)
   # Default output format: json
   ```
4. Verify:
   ```bash
   aws sts get-caller-identity
   ```

> Everything deploys to **`sa-east-1`** (lowest latency for LATAM, matches the CDK app).

---

## 2. Get an OpenAI API key and store it as a secret

The vision Lambda reads the key from SSM Parameter Store — it is **never** committed to git or
stored in CloudFormation.

1. Create a key at <https://platform.openai.com/api-keys> (and set a monthly spend limit under
   Billing → Limits so a bug can't run up a bill).
2. Store it as a **SecureString** parameter (the name the Lambda expects):
   ```bash
   aws ssm put-parameter \
     --name /photolist/openai-api-key \
     --type SecureString \
     --value "sk-...your-key..." \
     --region sa-east-1
   ```
   To rotate it later, re-run with `--overwrite`. CDK never touches this value.

---

## 3. One-time CDK bootstrap

CDK needs a small "toolkit" stack in the account/region before its first deploy:

```bash
cd infrastructure
npx --yes aws-cdk@2 bootstrap aws://<ACCOUNT_ID>/sa-east-1
```

(`<ACCOUNT_ID>` is the `Account` field from `aws sts get-caller-identity`.)

---

## 4. Build everything

From the repo root (with Java 21 on PATH — see step 0):

**Windows (PowerShell):**
```powershell
.\mvnw.cmd -f functions/analyze-photo/pom.xml package -q
.\mvnw.cmd -f functions/generate-upload-url/pom.xml package -q
.\mvnw.cmd -f infrastructure/pom.xml package -q
```

**Linux / macOS:**
```bash
./mvnw -f functions/analyze-photo/pom.xml package -q
./mvnw -f functions/generate-upload-url/pom.xml package -q
./mvnw -f infrastructure/pom.xml package -q
```

This produces:
- `functions/analyze-photo/target/analyze-photo.jar`
- `functions/generate-upload-url/target/generate-upload-url.jar`
- `infrastructure/target/photolist-infrastructure.jar` (the CDK app)

---

## 5. Deploy the AWS stacks

```bash
cd infrastructure
npx --yes aws-cdk@2 deploy --all --require-approval never
```

This creates three stacks:
- **PhotolistStorageStack** — S3 uploads bucket (24 h lifecycle) + DynamoDB cache
- **PhotolistApiStack** — both Lambdas + API Gateway (CORS enabled)
- **PhotolistFrontendStack** — private S3 site bucket + CloudFront distribution

When it finishes, CDK prints **outputs**. Copy these three values:

| Output | Used for |
|--------|----------|
| `PhotolistApiStack` → `PhotolistApiEndpoint...` (ends in `/prod/`) | Frontend `VITE_API_BASE_URL` |
| `PhotolistFrontendStack` → `FrontendBucketName` | Frontend upload target |
| `PhotolistFrontendStack` → `FrontendDistributionId` | Cache invalidation |
| `PhotolistFrontendStack` → `FrontendUrl` | The public site URL |

> If you didn't note them, fetch any time with:
> `aws cloudformation describe-stacks --stack-name PhotolistFrontendStack --query "Stacks[0].Outputs" --region sa-east-1`

---

## 6. Build and publish the frontend

1. Point the frontend at the live API. Create `frontend/.env.production`:
   ```
   VITE_API_BASE_URL=https://abc123.execute-api.sa-east-1.amazonaws.com/prod
   ```
   (Use the `PhotolistApiEndpoint` output **without** a trailing slash.)

2. Build it:
   ```bash
   cd frontend
   npm install
   npm run build      # outputs to frontend/dist
   ```

3. Upload to S3 and invalidate the CDN cache (substitute the two outputs from step 5):
   ```bash
   aws s3 sync dist "s3://<FrontendBucketName>/" --delete --region sa-east-1

   aws cloudfront create-invalidation \
     --distribution-id <FrontendDistributionId> \
     --paths "/*"
   ```

4. Open **`FrontendUrl`** in a browser. First propagation of a new CloudFront distribution can
   take a few minutes.

---

## 7. Smoke test

```bash
API=https://abc123.execute-api.sa-east-1.amazonaws.com/prod

# upload-url
curl -s -X POST "$API/upload-url" -H "Content-Type: application/json" \
  -d '{"contentType":"image/jpeg"}'
```

Then use the app end-to-end: open the site, take/upload a photo, confirm you get an item name
and MercadoLibre prices. (See `docs/api-contract.md` for full request/response shapes.)

---

## 8. Custom domain (optional)

1. Buy a domain (e.g. `photolist.lat`) at any registrar, or in **Route 53**.
2. Request an ACM certificate **in `us-east-1`** (CloudFront requires its certs there) for your
   domain.
3. Add the domain + certificate to the CloudFront distribution (console: distribution → Settings →
   Alternate domain names + Custom SSL certificate), or extend `PhotolistFrontendStack` with
   `domainNames` + `certificate` and redeploy.
4. Point a DNS record (Route 53 alias or a CNAME) at the CloudFront domain.

---

## 9. Updating after a code change

- **Backend change:** rebuild the affected jar (step 4) → `cdk deploy --all` (step 5).
- **Frontend change:** `npm run build` → `s3 sync` + invalidation (step 6.3).
- **Rotate the OpenAI key:** re-run the `put-parameter` command with `--overwrite` (step 2).

CI (`.github/workflows/deploy-dev.yml`) automates the backend path on push to `main` once you add
`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` repo secrets.

---

## 10. Teardown (stop all costs)

```bash
cd infrastructure
npx --yes aws-cdk@2 destroy --all
aws ssm delete-parameter --name /photolist/openai-api-key --region sa-east-1
```

The frontend S3 bucket must be empty before it can be deleted — if `destroy` complains, run
`aws s3 rm s3://<FrontendBucketName> --recursive` first.

---

## Troubleshooting

| Symptom | Cause / Fix |
|---------|-------------|
| `UnsupportedClassVersionError ... class file version 65.0` during `cdk deploy` | `java` on PATH is not 21. Re-do step 0. |
| `403` from S3 on photo upload | The PUT `Content-Type` didn't match the `contentType` sent to `/upload-url`. |
| `502` from `/analyze` | OpenAI key missing/invalid (check step 2) or MercadoLibre unreachable/rate-limited. |
| `422` from `/analyze` | Vision confidence < 0.5 — the item couldn't be identified. Try a clearer photo. |
| CORS error in browser | Confirm `VITE_API_BASE_URL` has no trailing slash and points at the `/prod` stage. |
| Frontend shows a blank/old page after deploy | Run the CloudFront invalidation (step 6.3). |

---

## Alternative: host the frontend on Vercel / Netlify (fallback)

If you prefer git-push deploys over `s3 sync`, you can skip `PhotolistFrontendStack` and host the
PWA on Vercel or Netlify free tier (backend stays on AWS):

1. Import the repo, set **Root directory** = `frontend`, build command `npm run build`, output `dist`.
2. Add env var `VITE_API_BASE_URL` = your `/prod` API URL.
3. Deploy. (You can then `cdk deploy PhotolistStorageStack PhotolistApiStack` only, omitting the
   frontend stack.)

This adds a second vendor account to manage; the all-AWS path above keeps everything on one bill.
