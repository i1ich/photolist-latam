# API Contract — PhotoList LATAM

Base URL: `https://{api-id}.execute-api.sa-east-1.amazonaws.com/prod`

---

## POST /upload-url

Generate a pre-signed S3 URL to upload a photo directly from the client.

### Request

```json
{
  "contentType": "image/jpeg"
}
```

### Response `200 OK`

```json
{
  "uploadUrl": "https://s3.amazonaws.com/photolist-photo-uploads/uploads/{uuid}.jpg?...",
  "imageKey": "uploads/{uuid}.jpg"
}
```

### Usage

```bash
# 1. Get upload URL
curl -X POST /upload-url -d '{"contentType":"image/jpeg"}'

# 2. Upload directly to S3
curl -X PUT "{uploadUrl}" \
  -H "Content-Type: image/jpeg" \
  --data-binary @photo.jpg
```

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
  "itemName": "iPhone 13 128GB",
  "category": "Smartphones y Accesorios",
  "site": "MLA",
  "priceRange": {
    "min": 450000,
    "max": 680000,
    "median": 550000,
    "currency": "ARS"
  },
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
}
```

### Error Responses

| Status | Description |
|--------|-------------|
| `400` | Missing `imageKey` or invalid `site` |
| `404` | Image not found in S3 |
| `422` | Item could not be identified from image |
| `500` | Internal error (Vision LLM or MercadoLibre unreachable) |

---

## Rate Limits

MVP: no rate limiting. To be added post-MVP via API Gateway usage plans.
