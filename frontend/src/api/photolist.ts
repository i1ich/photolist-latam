const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

export interface Listing {
  title: string
  price: number
  currency: string
  condition: string
  url: string
  thumbnail?: string
}

export interface ItemInfo {
  name: string
  brand?: string
  category?: string
  confidence: number
}

export interface MarketInfo {
  site: string
  currency: string
  priceMin: number
  priceMax: number
  priceMedian: number
  topListings: Listing[]
}

export interface AnalyzeResult {
  item: ItemInfo
  market: MarketInfo
  analyzedAt?: string
  cachedAt?: string | null
}

/**
 * Upload a photo and analyze it: returns the identified item plus MercadoLibre prices.
 *
 * Flow: request a pre-signed URL → PUT the file straight to S3 → POST /analyze.
 * The PUT Content-Type must match the contentType sent to /upload-url, otherwise
 * S3 rejects the upload with a signature mismatch.
 */
export async function analyzePhoto(file: File, site = 'MLA'): Promise<AnalyzeResult> {
  const contentType = file.type || 'image/jpeg'

  // Step 1: get pre-signed upload URL
  const uploadRes = await fetch(`${API_BASE}/upload-url`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ contentType })
  })

  if (!uploadRes.ok) throw new Error('No se pudo obtener la URL de subida')
  const { uploadUrl, imageKey } = await uploadRes.json()

  // Step 2: upload image directly to S3 (Content-Type must match the signed type)
  const s3Res = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': contentType },
    body: file
  })

  if (!s3Res.ok) throw new Error('No se pudo subir la imagen')

  // Step 3: analyze
  const analyzeRes = await fetch(`${API_BASE}/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ imageKey, site })
  })

  if (analyzeRes.status === 422) throw new Error('No se pudo identificar el artículo en la foto')
  if (!analyzeRes.ok) throw new Error('No se pudo analizar la imagen')
  return analyzeRes.json()
}
