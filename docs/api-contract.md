# API Contract — PhotoList LATAM

Base URL: `https://{api-id}.execute-api.sa-east-1.amazonaws.com/prod`

All responses include `Access-Control-Allow-Origin: *`, and the API Gateway answers
CORS preflight (`OPTIONS`) for both routes.

---

## POST /upload-url

Generate a pre-signed S3 URL to upload a photo directly from the client.

### Request

```json
{
  "contentType": "image/jpeg"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `contentType` | string | ✗ | One of `image/jpeg`, `image/png`, `image/webp`. Default: `image/jpeg` |

### Response `200 OK`

```json
{
  "uploadUrl": "https://{bucket}.s3.sa-east-1.amazonaws.com/uploads/{uuid}.jpg?X-Amz-...",
  "imageKey": "uploads/{uuid}.jpg",
  "expiresIn": 300
}
```

### Usage

```bash
# 1. Get upload URL
curl -X POST /upload-url -d '{"contentType":"image/jpeg"}'

# 2. Upload directly to S3 — the Content-Type MUST match the contentType above
curl -X PUT "{uploadUrl}" \
  -H "Content-Type: image/jpeg" \
  --data-binary @photo.jpg
```

> The `Content-Type` on the PUT must equal the `contentType` used to request the URL,
> otherwise S3 rejects the upload with a signature mismatch (403).

---

## POST /analyze

Analyze the uploaded photo: identify the item and return MercadoLibre listings with prices.

### Request

```json
{
  "imageKey": "uploads/{uuid}.jpg",
  "site": "MLA"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `imageKey` | string | ✓ | S3 key from `/upload-url` response |
| `site` | string | ✗ | MercadoLibre site: `MLA` (AR), `MLB` (BR), `MLM` (MX), `MLU` (UY). Default: `MLA` |

### Response `200 OK`

```json
{
  "item": {
    "name": "iPhone 13 128GB",
    "brand": "Apple",
    "category": "Celulares y Smartphones",
    "confidence": 0.92
  },
  "market": {
    "site": "MLA",
    "currency": "ARS",
    "priceMin": 450000,
    "priceMax": 680000,
    "priceMedian": 550000,
    "topListings": [
      {
        "title": "Apple iPhone 13 (128 Gb) - Negro",
        "price": 549999,
        "currency": "ARS",
        "condition": "used",
        "url": "https://www.mercadolibre.com.ar/...",
        "thumbnail": "https://http2.mlstatic.com/..."
      }
    ]
  },
  "analyzedAt": "2026-06-03T17:40:00Z",
  "cachedAt": null
}
```

`cachedAt` is non-null when the result was served from the DynamoDB cache (keyed by the
SHA-256 of `imageKey`, TTL 7 days).

### Error Responses

| Status | Description |
|--------|-------------|
| `400` | Missing `imageKey`, invalid JSON body, or unsupported `contentType` |
| `422` | Item could not be identified from the image (vision confidence < 0.5) |
| `502` | Upstream error (Vision LLM or MercadoLibre unreachable / rate-limited) |
| `500` | Internal error |

---

## Rate Limits

MVP: no application-level rate limiting. To be added post-MVP via API Gateway usage plans.
MercadoLibre 429 responses surface to the client as `502`.
