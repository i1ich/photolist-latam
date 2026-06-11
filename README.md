# PhotoList LATAM

**Photograph an item → AI identifies it → one click to its resale prices on MercadoLibre.**

🔗 **Live demo:** https://d1qbetwmt1ueqv.cloudfront.net

![Java 21](https://img.shields.io/badge/Java-21-orange) ![AWS CDK](https://img.shields.io/badge/IaC-AWS%20CDK-yellow) ![Lambda](https://img.shields.io/badge/Compute-AWS%20Lambda-ff9900) ![OpenAI Vision](https://img.shields.io/badge/AI-OpenAI%20Vision-412991) ![React](https://img.shields.io/badge/Frontend-React%20%2B%20Vite-61dafb) ![License: MIT](https://img.shields.io/badge/License-MIT-green)

<!-- TODO: replace with a real demo GIF: photo upload → "Laptop · Apple · 90% confianza" → "Ver precios en MercadoLibre" -->
<!-- ![Demo](docs/demo.gif) -->

---

## What it does

The used-goods market in Latin America runs on MercadoLibre, but pricing an item for resale means guessing search keywords and scrolling listings. PhotoList removes that friction:

1. **Snap or upload a photo** (PWA, works from a phone camera)
2. A **Vision LLM** identifies the item — name, brand, category, confidence score
3. You get a **direct link to live MercadoLibre listings** for that exact item, in your country's marketplace and currency

Spanish-language UI, LATAM-first (MLU 🇺🇾 / MLA 🇦🇷 / MLB 🇧🇷 / MLM 🇲🇽).

## Architecture

Fully serverless — no always-on servers, ~$0/month on the AWS free tier at MVP traffic. The only meaningful cost is the Vision LLM (~$5–15/month per 1,000 photos).

![schema-latam.svg](docs/schema-latam-photolist.svg)

**Key engineering decisions:**

- **Presigned S3 uploads** — images go browser → S3 directly; the API never proxies binary payloads
- **Secrets in SSM Parameter Store** (SecureString) — no keys in code, env vars, or CloudFormation
- **Model config in SSM** — switch the vision model or token budget without redeploying
- **DynamoDB result caching** — repeated analyses don't re-spend LLM tokens
- **Readable failure modes** — external-service errors surface as `502` with the upstream message, low-confidence identification as `422`; `500` is reserved for actual bugs
- **AWS CDK in Java** — one language for application code and infrastructure, 3 stacks (Storage / Api / Frontend)

## Why a deep-link instead of prices in the response?

The original design called MercadoLibre's `GET /sites/{site}/search` to return a price range and top listings inline. During integration, that endpoint returned `403` for every request. Rather than guess, I ran a hypothesis matrix — datacenter IP blocking, site-specific restrictions, token type (app vs. user), missing OAuth scopes, app status — and falsified each one by testing from residential and AWS IPs, with and without tokens of both grant types, across MLA and MLU. The remaining explanation, confirmed by ML's own app metadata (`certified_status: not_certified`), is that MercadoLibre now restricts catalog search to applications certified through its Developer Partner Program — a weeks-to-months process requiring real seller GMV.

So the MVP ships with a generated deep-link to the public search results page instead: the user still lands on live prices in one click, and the OAuth/token-rotation work remains in the git history for the certified version. Diagnosing this properly also avoided a useless ~$40/month NAT Gateway "fix."

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, AWS Lambda, API Gateway |
| AI | OpenAI Vision (model configurable via SSM), structured JSON output |
| Storage | S3 (24h lifecycle), DynamoDB (TTL cache) |
| Frontend | React + TypeScript + Vite, PWA, served via CloudFront |
| IaC | AWS CDK (Java) — `PhotolistStorageStack`, `PhotolistApiStack`, `PhotolistFrontendStack` |
| CI/CD | GitHub Actions (backend deploy on push to `main`) |
| Tests | JUnit 5 + Cucumber (Gherkin) for pricing logic |

## API

```
POST /upload-url   → { uploadUrl, imageKey }        # presigned S3 PUT
POST /analyze      → { item: { name, brand, category, confidence },
                       market: { site, searchUrl },
                       analyzedAt }
```

## Quick start

**Prerequisites:** Java 21, Node 20+, AWS CLI v2. Maven is not required — the repo ships the Maven Wrapper.

```bash
# Build Lambdas + CDK app
./mvnw -f functions/analyze-photo/pom.xml package -q
./mvnw -f functions/generate-upload-url/pom.xml package -q
./mvnw -f infrastructure/pom.xml package -q

# Deploy (one-time bootstrap first — see docs)
cd infrastructure
npx --yes aws-cdk@2 deploy --all --require-approval never
```

Full walkthrough from a clean machine to a live URL (account setup, secrets, frontend publish, teardown): **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)**

> Windows note: the CDK CLI uses the `java` on your **PATH** (it ignores `JAVA_HOME`) — make sure it resolves to Java 21.

## Roadmap

- [ ] MercadoLibre Developer Partner certification → inline price range + top listings in the response
- [ ] Content-hash cache keys (currently keyed per-upload, limiting cache reuse)
- [ ] Custom domain
- [ ] MercadoLibre affiliate links

## License

MIT — see [LICENSE](LICENSE).

---

*Built solo in 3 weeks by [Ilia Doinikov](https://www.linkedin.com/in/ilia-doinikov-d) — Senior Java/AWS engineer, Montevideo 🇺🇾.*
