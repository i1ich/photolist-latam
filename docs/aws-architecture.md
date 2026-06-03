# AWS Architecture — PhotoList LATAM

## Overview

Region: `sa-east-1` (São Paulo) — minimum latency for LATAM.

## Flow

```
[Mobile PWA]
     │
     │ POST /upload-url
     ▼
[API Gateway]──►[Lambda: generate-upload-url]──►[S3 pre-signed URL]
     │
     │ PUT (direct to S3)
     ▼
[S3: photo-uploads] ──► (24h lifecycle)
     │
     │ POST /analyze {imageKey}
     ▼
[API Gateway]──►[Lambda: analyze-photo]
                     │
                     ├──►[S3 GetObject] ──► image bytes
                     │
                     ├──►[OpenAI GPT-4o mini Vision API]
                     │       └── returns: item name, category
                     │
                     ├──►[MercadoLibre Search API]
                     │       └── GET /sites/MLA/search?q={itemName}
                     │           returns: top listings with prices
                     │
                     ├──►[DynamoDB: results-cache]
                     │       └── cache by SHA-256(imageKey) (TTL: 7 days)
                     │
                     └──► AnalyzeResponse

[Browser PWA] is served from:
[S3: frontend bucket (private)] ──► [CloudFront (HTTPS, CDN)] ──► user
```

The OpenAI API key lives in SSM Parameter Store (`/photolist/openai-api-key`,
SecureString) and is created out-of-band so CloudFormation never stores the secret.

## AWS Resources

| Resource | Name | Purpose |
|----------|------|---------|
| S3 Bucket | `photolist-photo-uploads` | Temporary photo storage (24h TTL) |
| DynamoDB | `photolist-results-cache` | Cache analysis results |
| Lambda | `photolist-generate-upload-url` | Generate S3 pre-signed PUT URL |
| Lambda | `photolist-analyze-photo` | Orchestrate Vision LLM + ML search |
| API Gateway | `photolist-api` | REST API entry point (CORS enabled) |
| S3 Bucket | frontend (private) | Built PWA assets |
| CloudFront | frontend distribution | HTTPS CDN for the PWA |
| SSM Parameter | `/photolist/openai-api-key` | OpenAI key (SecureString, created out-of-band) |

## Cost Estimate (MVP, 1000 req/day)

| Service | Est. cost/month |
|---------|----------------|
| Lambda (2 functions) | ~$0 (free tier) |
| API Gateway | ~$3.50 |
| S3 storage | ~$0.01 |
| DynamoDB | ~$0 (free tier) |
| OpenAI GPT-4o mini | ~$5–15 (depends on usage) |
| **Total** | **~$10–20/month** |
